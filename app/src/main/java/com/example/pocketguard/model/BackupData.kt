package com.example.pocketguard.model

data class BackupData(
    val transactions: List<Transaction>,
    val budgetCategories: List<BudgetCategory>,
    val totalBudgets: Map<String, Double>,
    val passcode: String,
    val biometricEnabled: Boolean,
    val budgetAlertsEnabled: Boolean,
    val transactionAlertsEnabled: Boolean,
    val currency: String
) 