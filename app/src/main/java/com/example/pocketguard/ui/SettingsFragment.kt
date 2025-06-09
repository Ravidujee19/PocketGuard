package com.example.pocketguard.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pocketguard.R
import com.example.pocketguard.data.SharedPreferencesManager
import com.example.pocketguard.model.BackupData
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferencesManager = SharedPreferencesManager(requireContext())
        setupViews(view)
    }

    private fun setupViews(view: View) {
        // Currency settings
        val spinnerCurrency = view.findViewById<AutoCompleteTextView>(R.id.spinnerCurrency)
        val currencies = arrayOf("LKR", "USD", "EUR", "GBP", "JPY", "INR")
        val currencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        spinnerCurrency.setAdapter(currencyAdapter)
        spinnerCurrency.setText(sharedPreferencesManager.getCurrency(), false)
        spinnerCurrency.setOnItemClickListener { _, _, position, _ ->
            sharedPreferencesManager.setCurrency(currencies[position])
            Toast.makeText(context, "Currency updated to ${currencies[position]}", Toast.LENGTH_SHORT).show()
        }

        // Security settings
        val btnChangePasscode = view.findViewById<MaterialButton>(R.id.btnChangePasscode)
        btnChangePasscode.setOnClickListener {
            showChangePasscodeDialog()
        }

        // Data management
        val btnBackup = view.findViewById<MaterialButton>(R.id.btnBackup)
        val btnRestore = view.findViewById<MaterialButton>(R.id.btnRestore)

        btnBackup.setOnClickListener {
            showBackupConfirmationDialog()
        }

        btnRestore.setOnClickListener {
            showRestoreConfirmationDialog()
        }
    }

    private fun showChangePasscodeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_passcode, null)
        val etCurrentPasscode = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCurrentPasscode)
        val etNewPasscode = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewPasscode)
        val etConfirmPasscode = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPasscode)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Passcode")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val currentPasscode = etCurrentPasscode.text.toString()
                val newPasscode = etNewPasscode.text.toString()
                val confirmPasscode = etConfirmPasscode.text.toString()

                if (currentPasscode != sharedPreferencesManager.getPasscode()) {
                    Toast.makeText(context, "Current passcode is incorrect", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPasscode.length != 4) {
                    Toast.makeText(context, "Passcode must be 4 digits", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPasscode != confirmPasscode) {
                    Toast.makeText(context, "Passcodes do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                sharedPreferencesManager.savePasscode(newPasscode)
                Toast.makeText(context, "Passcode changed successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBackupConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Backup")
            .setMessage("This will create a backup of all your data. Continue?")
            .setPositiveButton("Backup") { _, _ ->
                exportData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRestoreConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore from Backup")
            .setMessage("This will replace all current data with the backup data. Continue?")
            .setPositiveButton("Restore") { _, _ ->
                importData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportData() {
        try {
            val backupData = sharedPreferencesManager.createBackup()
            val gson = Gson()
            val jsonString = gson.toJson(backupData)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "pocketguard_backup_$timestamp.json"
            val file = File(requireContext().filesDir, fileName)

            file.writeText(jsonString)

            Toast.makeText(context, "Backup created successfully: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error creating backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importData() {
        try {
            val backupFiles = requireContext().filesDir.listFiles { file ->
                file.name.startsWith("pocketguard_backup_") && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }

            if (backupFiles.isNullOrEmpty()) {
                Toast.makeText(context, "No backup files found", Toast.LENGTH_SHORT).show()
                return
            }

            // Get the most recent backup file
            val latestBackup = backupFiles.first()
            val jsonString = latestBackup.readText()

            val gson = Gson()
            val type = object : TypeToken<BackupData>() {}.type
            val backupData = gson.fromJson<BackupData>(jsonString, type)

            // Restore data
            sharedPreferencesManager.clearAllData()
            sharedPreferencesManager.restoreFromBackup(backupData)

            // Notify parent activity to refresh budget fragment
            (activity as? FragmentCallback)?.onTransactionChanged()

            // Force refresh the budget fragment if it's currently visible
            val budgetFragment = parentFragmentManager.fragments.find { it is BudgetFragment }
            if (budgetFragment is BudgetFragment) {
                budgetFragment.loadBudgetData()
            }

            Toast.makeText(context, "Data restored successfully from ${latestBackup.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error restoring backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 