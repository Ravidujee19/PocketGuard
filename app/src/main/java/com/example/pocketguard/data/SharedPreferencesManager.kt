package com.example.pocketguard.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.pocketguard.model.Transaction
import com.example.pocketguard.model.BudgetCategory
import com.example.pocketguard.model.BackupData
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.*

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val context = context.applicationContext

    companion object {
        private const val PREFS_NAME = "PocketGuardPrefs"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_BUDGET_CATEGORIES = "budget_categories"
        private const val KEY_PASSCODE = "passcode"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_BUDGET_ALERTS = "budget_alerts"
        private const val KEY_TRANSACTION_ALERTS = "transaction_alerts"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_TOTAL_BUDGET = "total_budget_"
        private const val KEY_LAST_RESET_MONTH = "last_reset_month"
        private const val KEY_LAST_RESET_YEAR = "last_reset_year"
        private const val BACKUP_FILE_NAME = "pocketguard_backup.json"
    }

    // Currency Settings
    fun setCurrency(currencyCode: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY, currencyCode).apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, "LKR") ?: "LKR"
    }

    fun getCurrencySymbol(): String {
        return when (getCurrency()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "INR" -> "₹"
            "LKR" -> "Rs."
            else -> "Rs."
        }
    }

    fun getCurrencyLocale(): Locale {
        return when (getCurrency()) {
            "USD" -> Locale.US
            "EUR" -> Locale.GERMANY
            "GBP" -> Locale.UK
            "JPY" -> Locale.JAPAN
            "INR" -> Locale("en", "IN")
            "LKR" -> Locale("en", "LK")
            else -> Locale("en", "LK")
        }
    }

    // Transactions
    fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, json).apply()
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(KEY_TRANSACTIONS, null)
        return if (json != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    // Budget Categories
    fun saveBudgetCategories(categories: List<BudgetCategory>) {
        val json = gson.toJson(categories)
        sharedPreferences.edit().putString(KEY_BUDGET_CATEGORIES, json).apply()
    }

    fun getBudgetCategories(): List<BudgetCategory> {
        val json = sharedPreferences.getString(KEY_BUDGET_CATEGORIES, null)
        return if (json != null) {
            val type = object : TypeToken<List<BudgetCategory>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    // Passcode
    fun savePasscode(passcode: String) {
        sharedPreferences.edit().putString(KEY_PASSCODE, passcode).apply()
    }

    fun getPasscode(): String {
        return sharedPreferences.getString(KEY_PASSCODE, "1234") ?: "1234"
    }

    // Settings
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BUDGET_ALERTS, enabled).apply()
    }

    fun isBudgetAlertsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BUDGET_ALERTS, true)
    }

    fun setTransactionAlertsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_TRANSACTION_ALERTS, enabled).apply()
    }

    fun isTransactionAlertsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_TRANSACTION_ALERTS, true)
    }

    // Budget Methods
    fun setTotalBudget(amount: Double, month: Int, year: Int) {
        val key = "$KEY_TOTAL_BUDGET${month}_$year"
        sharedPreferences.edit().putString(key, amount.toString()).apply()
    }

    fun getTotalBudget(month: Int, year: Int): Double {
        val key = "$KEY_TOTAL_BUDGET${month}_$year"
        return sharedPreferences.getString(key, "0.0")?.toDoubleOrNull() ?: 0.0
    }

    fun resetMonthlyBudget() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Reset total budget
        setTotalBudget(0.0, currentMonth, currentYear)
        
        // Reset category budgets
        val categories = getBudgetCategories().toMutableList()
        categories.removeAll { it.month == currentMonth && it.year == currentYear }
        saveBudgetCategories(categories)
    }

    fun updateCategorySpent(category: String, amount: Double, month: Int, year: Int) {
        val categories = getBudgetCategories().toMutableList()
        val index = categories.indexOfFirst { 
            it.name == category && it.month == month && it.year == year 
        }
        
        if (index != -1) {
            val updatedCategory = categories[index].copy(spent = categories[index].spent + amount)
            categories[index] = updatedCategory
            saveBudgetCategories(categories)
        }
    }

    fun getCategorySpent(category: String, month: Int, year: Int): Double {
        return getBudgetCategories()
            .find { it.name == category && it.month == month && it.year == year }
            ?.spent ?: 0.0
    }

    fun getTotalSpent(month: Int, year: Int): Double {
        return getBudgetCategories()
            .filter { it.month == month && it.year == year }
            .sumOf { it.spent }
    }

    fun validateCategoryBudget(category: BudgetCategory): Boolean {
        val totalBudget = getTotalBudget(category.month, category.year)
        val currentCategories = getBudgetCategories()
            .filter { it.month == category.month && it.year == category.year && it.id != category.id }
        val totalCategoryBudgets = currentCategories.sumOf { it.amount }
        
        return (totalCategoryBudgets + category.amount) <= totalBudget
    }

    // Helper methods for transactions
    fun addTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        // Ensure unique ID by using timestamp
        val newTransaction = transaction.copy(id = System.currentTimeMillis())
        transactions.add(newTransaction)
        saveTransactions(transactions)
    }

    fun updateTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            // Preserve the original ID when updating
            transactions[index] = transaction
            saveTransactions(transactions)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        // Find the transaction with matching ID
        val transactionToDelete = transactions.find { it.id == transaction.id }
        if (transactionToDelete != null) {
            transactions.remove(transactionToDelete)
            saveTransactions(transactions)
        }
    }

    // Helper methods for budget categories
    fun addBudgetCategory(category: BudgetCategory) {
        val categories = getBudgetCategories().toMutableList()
        categories.add(category)
        saveBudgetCategories(categories)
    }

    fun updateBudgetCategory(category: BudgetCategory) {
        val categories = getBudgetCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) {
            categories[index] = category
            saveBudgetCategories(categories)
        }
    }

    fun deleteBudgetCategory(category: BudgetCategory) {
        val categories = getBudgetCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) {
            categories.removeAt(index)
            saveBudgetCategories(categories)
        }
    }

    // Backup and Restore
    fun createBackup(): BackupData {
        return BackupData(
            transactions = getTransactions(),
            budgetCategories = getBudgetCategories(),
            totalBudgets = getAllTotalBudgets(),
            passcode = getPasscode(),
            biometricEnabled = isBiometricEnabled(),
            budgetAlertsEnabled = isBudgetAlertsEnabled(),
            transactionAlertsEnabled = isTransactionAlertsEnabled(),
            currency = getCurrency()
        )
    }

    fun restoreFromBackup(backupData: BackupData) {
        val editor = sharedPreferences.edit()
        
        // Restore transactions
        editor.putString(KEY_TRANSACTIONS, gson.toJson(backupData.transactions))
        
        // Restore budget categories
        editor.putString(KEY_BUDGET_CATEGORIES, gson.toJson(backupData.budgetCategories))
        
        // Restore total budgets
        backupData.totalBudgets.forEach { (key, value) ->
            editor.putString(key, value.toString())
        }

        // Restore settings
        editor.putString(KEY_PASSCODE, backupData.passcode)
        editor.putBoolean(KEY_BIOMETRIC_ENABLED, backupData.biometricEnabled)
        editor.putBoolean(KEY_BUDGET_ALERTS, backupData.budgetAlertsEnabled)
        editor.putBoolean(KEY_TRANSACTION_ALERTS, backupData.transactionAlertsEnabled)
        editor.putString(KEY_CURRENCY, backupData.currency)
        
        editor.apply()
    }

    fun setLastResetMonth(month: Int) {
        sharedPreferences.edit().putInt(KEY_LAST_RESET_MONTH, month).apply()
    }

    fun getLastResetMonth(): Int {
        return sharedPreferences.getInt(KEY_LAST_RESET_MONTH, -1)
    }

    fun setLastResetYear(year: Int) {
        sharedPreferences.edit().putInt(KEY_LAST_RESET_YEAR, year).apply()
    }

    fun getLastResetYear(): Int {
        return sharedPreferences.getInt(KEY_LAST_RESET_YEAR, -1)
    }

    fun clearAllData() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    fun getAllTotalBudgets(): Map<String, Double> {
        val budgets = mutableMapOf<String, Double>()
        val allPrefs = sharedPreferences.all
        
        allPrefs.forEach { (key, value) ->
            if (key.startsWith(KEY_TOTAL_BUDGET)) {
                budgets[key] = value.toString().toDouble()
            }
        }
        
        return budgets
    }
} 