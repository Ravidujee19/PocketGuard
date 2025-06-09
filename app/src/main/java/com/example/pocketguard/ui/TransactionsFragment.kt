package com.example.pocketguard.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pocketguard.R
import com.example.pocketguard.data.SharedPreferencesManager
import com.example.pocketguard.model.Transaction
import com.example.pocketguard.ui.adapters.TransactionAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import java.util.*

class TransactionsFragment : Fragment() {
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private lateinit var transactionAdapter: TransactionAdapter
    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferencesManager = SharedPreferencesManager(requireContext())

        setupRecyclerView()
        setupTabLayout()
        setupFab()
        loadTransactions()
    }

    private fun setupRecyclerView() {
        val recyclerView = view?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvTransactions)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        transactionAdapter = TransactionAdapter(
            emptyList(),
            onEditClick = { transaction -> showEditTransactionDialog(transaction) },
            onDeleteClick = { transaction -> showDeleteConfirmationDialog(transaction) }
        )
        recyclerView?.adapter = transactionAdapter
    }

    private fun setupTabLayout() {
        view?.findViewById<TabLayout>(R.id.tabLayout)?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                loadTransactions()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFab() {
        view?.findViewById<FloatingActionButton>(R.id.fabAddTransaction)?.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun loadTransactions() {
        val transactions = sharedPreferencesManager.getTransactions()
        val selectedTab = view?.findViewById<TabLayout>(R.id.tabLayout)?.selectedTabPosition ?: 0

        val filteredTransactions = when (selectedTab) {
            1 -> transactions.filter { it.type == "INCOME" }
            2 -> transactions.filter { it.type == "EXPENSE" }
            else -> transactions
        }.sortedByDescending { it.date }

        transactionAdapter.updateTransactions(filteredTransactions)
    }

    private fun showAddTransactionDialog() {
        showTransactionDialog(null)
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        showTransactionDialog(transaction)
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                // If it's an expense, subtract the amount from the budget category
                if (transaction.type == "EXPENSE") {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = transaction.date
                    val month = calendar.get(Calendar.MONTH)
                    val year = calendar.get(Calendar.YEAR)
                    sharedPreferencesManager.updateCategorySpent(
                        transaction.category,
                        -transaction.amount,
                        month,
                        year
                    )
                }
                
                sharedPreferencesManager.deleteTransaction(transaction)
                loadTransactions()
                // Notify parent activity to refresh budget fragment
                (activity as? FragmentCallback)?.onTransactionChanged()
                Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTransactionDialog(transaction: Transaction?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_transaction, null)
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTitle)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAmount)
        val rgTransactionType = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgTransactionType)
        val spinnerCategory = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerCategory)
        val btnDate = dialogView.findViewById<android.widget.Button>(R.id.btnDate)

        // Set up category spinner
        val categories = if (rgTransactionType.checkedRadioButtonId == R.id.rbIncome) {
            resources.getStringArray(R.array.income_categories)
        } else {
            resources.getStringArray(R.array.expense_categories)
        }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Update categories when transaction type changes
        rgTransactionType.setOnCheckedChangeListener { _, checkedId ->
            val newCategories = if (checkedId == R.id.rbIncome) {
                resources.getStringArray(R.array.income_categories)
            } else {
                resources.getStringArray(R.array.expense_categories)
            }
            val newAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, newCategories)
            newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = newAdapter
        }

        // Set initial values if editing
        transaction?.let {
            etTitle.setText(it.description)
            etAmount.setText(it.amount.toString())
            if (it.type == "EXPENSE") {
                rgTransactionType.check(R.id.rbExpense)
            }
            selectedDate = it.date
            // Set the category in spinner
            val categoryPosition = categories.indexOf(it.category)
            if (categoryPosition != -1) {
                spinnerCategory.setSelection(categoryPosition)
            }
        }

        // Update date button text
        updateDateButtonText(btnDate)

        // Show date picker
        btnDate.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                updateDateButtonText(btnDate)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (transaction == null) "Add Transaction" else "Edit Transaction")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                try {
                    val title = etTitle.text.toString()
                    val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                    val category = spinnerCategory.selectedItem.toString()
                    val type = if (rgTransactionType.checkedRadioButtonId == R.id.rbIncome) "INCOME" else "EXPENSE"

                    if (title.isBlank() || amount <= 0) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val newTransaction = transaction?.copy(
                        description = title,
                        amount = amount,
                        category = category,
                        type = type,
                        date = selectedDate
                    ) ?: Transaction(
                        description = title,
                        amount = amount,
                        category = category,
                        type = type,
                        date = selectedDate
                    )

                    if (transaction == null) {
                        // Adding new transaction
                        sharedPreferencesManager.addTransaction(newTransaction)
                        
                        // Update budget category spent amount if it's an expense
                        if (type == "EXPENSE") {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = selectedDate
                            val month = calendar.get(Calendar.MONTH)
                            val year = calendar.get(Calendar.YEAR)
                            sharedPreferencesManager.updateCategorySpent(
                                category,
                                amount,
                                month,
                                year
                            )
                        }
                        Toast.makeText(context, "Transaction added", Toast.LENGTH_SHORT).show()
                    } else {
                        // Updating existing transaction
                        
                        // First remove old expense amount if it was an expense
                        if (transaction.type == "EXPENSE") {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = transaction.date
                            val month = calendar.get(Calendar.MONTH)
                            val year = calendar.get(Calendar.YEAR)
                            sharedPreferencesManager.updateCategorySpent(
                                transaction.category,
                                -transaction.amount,
                                month,
                                year
                            )
                        }
                        
                        // Update the transaction
                        sharedPreferencesManager.updateTransaction(newTransaction)
                        
                        // Add new expense amount if it's an expense
                        if (type == "EXPENSE") {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = selectedDate
                            val month = calendar.get(Calendar.MONTH)
                            val year = calendar.get(Calendar.YEAR)
                            sharedPreferencesManager.updateCategorySpent(
                                category,
                                amount,
                                month,
                                year
                            )
                        }
                        Toast.makeText(context, "Transaction updated", Toast.LENGTH_SHORT).show()
                    }

                    loadTransactions()
                    // Notify parent activity to refresh budget fragment
                    (activity as? FragmentCallback)?.onTransactionChanged()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                val selectedDateMillis = calendar.timeInMillis
                
                // Check if selected date is in the future
                if (selectedDateMillis > System.currentTimeMillis()) {
                    Toast.makeText(context, "Cannot select future date", Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }
                
                onDateSelected(selectedDateMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set maximum date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        
        datePickerDialog.show()
    }

    private fun updateDateButtonText(button: android.widget.Button) {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        button.text = dateFormat.format(Date(selectedDate))
    }
} 