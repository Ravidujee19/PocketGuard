package com.example.pocketguard.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketguard.R
import com.example.pocketguard.data.SharedPreferencesManager
import com.example.pocketguard.model.BudgetCategory
import com.example.pocketguard.ui.adapters.BudgetCategoryAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.*

class BudgetFragment : Fragment() {
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private lateinit var etTotalBudget: TextInputEditText
    private lateinit var progressBudget: LinearProgressIndicator
    private lateinit var tvBudgetProgress: TextView
    private lateinit var tvBudgetWarning: TextView
    private lateinit var rvCategoryBudgets: RecyclerView
    private lateinit var categoryAdapter: BudgetCategoryAdapter
    private val CHANNEL_ID = "budget_notification_channel"
    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 123

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkNotificationPermission()
        createNotificationChannel()
        sharedPreferencesManager = SharedPreferencesManager(requireContext())
        initializeViews(view)
        setupListeners()
        checkAndResetMonthlyBudget()
        loadBudgetData()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is granted, create notification channel
                    createNotificationChannel()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation why the permission is needed
                    showNotificationPermissionRationale()
                }
                else -> {
                    // Request the permission
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        } else {
            // For Android 12 and below, create notification channel directly
            createNotificationChannel()
        }
    }

    private fun showNotificationPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Notification Permission Required")
            .setMessage("This app needs notification permission to alert you about budget limits and warnings.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, create notification channel
                    createNotificationChannel()
                } else {
                    // Permission denied, show a message
                    Toast.makeText(
                        context,
                        "Notification permission is required for budget alerts",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun initializeViews(view: View) {
        etTotalBudget = view.findViewById(R.id.etTotalBudget)
        progressBudget = view.findViewById(R.id.progressBudget)
        tvBudgetProgress = view.findViewById(R.id.tvBudgetProgress)
        tvBudgetWarning = view.findViewById(R.id.tvBudgetWarning)
        rvCategoryBudgets = view.findViewById(R.id.rvCategoryBudgets)

        rvCategoryBudgets.layoutManager = LinearLayoutManager(context)
        categoryAdapter = BudgetCategoryAdapter(
            emptyList(),
            onEditClick = { category -> showEditCategoryDialog(category) },
            onDeleteClick = { category -> showDeleteCategoryDialog(category) }
        )
        rvCategoryBudgets.adapter = categoryAdapter
    }

    private fun setupListeners() {
        val btnSaveBudget = view?.findViewById<MaterialButton>(R.id.btnSaveBudget)
        val btnAddCategory = view?.findViewById<MaterialButton>(R.id.btnAddCategory)

        btnSaveBudget?.setOnClickListener {
            saveTotalBudget()
        }

        btnAddCategory?.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Notifications"
            val descriptionText = "Notifications for budget limits"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), null)
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showBudgetLimitNotification(category: String) {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val totalBudget = sharedPreferencesManager.getTotalBudget(currentMonth, currentYear)
        val totalSpent = sharedPreferencesManager.getTotalSpent(currentMonth, currentYear)
        val totalProgress = if (totalBudget > 0) {
            ((totalSpent / totalBudget) * 100).toInt().coerceIn(0, 200)
        } else {
            0
        }

        // For category notifications
        if (category != "Total Budget") {
            val categorySpent = sharedPreferencesManager.getCategorySpent(category, currentMonth, currentYear)
            val categoryBudget = sharedPreferencesManager.getBudgetCategories()
                .find { it.name == category && it.month == currentMonth && it.year == currentYear }
                ?.amount ?: 0.0
            val categoryProgress = if (categoryBudget > 0) {
                ((categorySpent / categoryBudget) * 100).toInt().coerceIn(0, 200)
            } else {
                0
            }

            val title = when {
                categoryProgress >= 100 -> "Category Budget Exceeded!"
                categoryProgress >= 90 -> "Category Budget Warning"
                else -> return
            }

            val message = when {
                categoryProgress >= 100 -> "Your $category budget has been exceeded"
                else -> "Your $category budget is nearly exceeded"
            }

            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(false)
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            notificationManager.notify(category.hashCode(), notification)
        }
        
        // For total budget notifications
        if (totalProgress >= 90) {
            val title = when {
                totalProgress >= 100 -> "Total Budget Exceeded!"
                else -> "Total Budget Warning"
            }

            val message = when {
                totalProgress >= 100 -> {
                    val exceededBy = (totalProgress - 100).coerceIn(0, 100)
                    "You have exceeded your total budget by $exceededBy%"
                }
                else -> "You have used $totalProgress% of your total budget"
            }

            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(false)
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            notificationManager.notify("Total Budget".hashCode(), notification)
        }
    }

    fun loadBudgetData() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Load total budget
        val totalBudget = sharedPreferencesManager.getTotalBudget(currentMonth, currentYear)
        etTotalBudget.setText(totalBudget.toString())

        // Load category budgets
        val budgetCategories = sharedPreferencesManager.getBudgetCategories()
            .filter { it.month == currentMonth && it.year == currentYear }
        
        val updatedCategories = budgetCategories.map { category ->
            val spent = sharedPreferencesManager.getCategorySpent(category.name, currentMonth, currentYear)
            val updatedCategory = category.copy(spent = spent)
            
            // Check if spending reaches 90% or 100% of category budget
            val categoryProgress = if (category.amount > 0) {
                ((spent / category.amount) * 100).toInt().coerceIn(0, 200)  // Cap at 200%
            } else {
                0
            }
            
            if (categoryProgress >= 90) {
                showBudgetLimitNotification(category.name)
            }
            
            updatedCategory
        }

        // Calculate total spent
        val totalSpent = updatedCategories.sumOf { it.spent }
        
        // Update progress
        updateBudgetProgress(totalBudget, totalSpent)
        
        // Update category list
        updateCategoryList(updatedCategories)

        // Force refresh the RecyclerView
        categoryAdapter.notifyDataSetChanged()
    }

    private fun checkAndResetMonthlyBudget() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Check if we need to reset the budget for the new month
        val lastResetMonth = sharedPreferencesManager.getLastResetMonth()
        val lastResetYear = sharedPreferencesManager.getLastResetYear()
        
        if (lastResetMonth != currentMonth || lastResetYear != currentYear) {
            sharedPreferencesManager.resetMonthlyBudget()
            sharedPreferencesManager.setLastResetMonth(currentMonth)
            sharedPreferencesManager.setLastResetYear(currentYear)
        }
    }

    private fun saveTotalBudget() {
        val budgetAmount = etTotalBudget.text.toString().toDoubleOrNull() ?: 0.0
        if (budgetAmount <= 0) {
            Toast.makeText(context, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        sharedPreferencesManager.setTotalBudget(budgetAmount, currentMonth, currentYear)
        loadBudgetData()
        Toast.makeText(context, "Budget saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun updateBudgetProgress(totalBudget: Double, totalSpent: Double) {
        val progress = if (totalBudget > 0) {
            (totalSpent / totalBudget * 100).toInt()
        } else {
            0
        }
        
        progressBudget.progress = progress
        tvBudgetProgress.text = "$progress% of budget used"

        // Show warning if budget is nearly exceeded
        when {
            progress >= 100 -> {
                tvBudgetWarning.text = "Budget exceeded!"
                tvBudgetWarning.visibility = View.VISIBLE
                showBudgetLimitNotification("Total Budget")
            }
            progress >= 90 -> {
                tvBudgetWarning.text = "Warning: Budget nearly exceeded"
                tvBudgetWarning.visibility = View.VISIBLE
                showBudgetLimitNotification("Total Budget")
            }
            else -> {
                tvBudgetWarning.visibility = View.GONE
            }
        }
    }

    private fun updateCategoryList(categories: List<BudgetCategory>) {
        categoryAdapter.updateCategories(categories)
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget_category, null)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val etCategoryAmount = dialogView.findViewById<TextInputEditText>(R.id.etCategoryAmount)

        // Set up category spinner
        val categories = resources.getStringArray(R.array.expense_categories)
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Budget Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = spinnerCategory.selectedItem.toString()
                val amount = etCategoryAmount.text.toString().toDoubleOrNull() ?: 0.0

                if (amount <= 0) {
                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                val category = BudgetCategory(
                    name = name,
                    amount = amount,
                    month = currentMonth,
                    year = currentYear
                )

                if (!sharedPreferencesManager.validateCategoryBudget(category)) {
                    Toast.makeText(context, "Category budget exceeds total budget", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                sharedPreferencesManager.addBudgetCategory(category)
                loadBudgetData()
                Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCategoryDialog(category: BudgetCategory) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget_category, null)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val etCategoryAmount = dialogView.findViewById<TextInputEditText>(R.id.etCategoryAmount)

        // Set up category spinner
        val categories = resources.getStringArray(R.array.expense_categories)
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Set initial values
        val categoryPosition = categories.indexOf(category.name)
        if (categoryPosition != -1) {
            spinnerCategory.setSelection(categoryPosition)
        }
        etCategoryAmount.setText(category.amount.toString())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Budget Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = spinnerCategory.selectedItem.toString()
                val amount = etCategoryAmount.text.toString().toDoubleOrNull() ?: 0.0

                if (amount <= 0) {
                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedCategory = category.copy(
                    name = name,
                    amount = amount
                )

                if (!sharedPreferencesManager.validateCategoryBudget(updatedCategory)) {
                    Toast.makeText(context, "Category budget exceeds total budget", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                sharedPreferencesManager.updateBudgetCategory(updatedCategory)
                loadBudgetData()
                Toast.makeText(context, "Category updated successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteCategoryDialog(category: BudgetCategory) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category?")
            .setPositiveButton("Delete") { _, _ ->
                sharedPreferencesManager.deleteBudgetCategory(category)
                loadBudgetData()
                Toast.makeText(context, "Category deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun refreshBudgetData() {
        loadBudgetData()
    }
} 