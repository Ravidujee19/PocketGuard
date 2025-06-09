package com.example.pocketguard.model

data class Transaction(
    val id: Long = System.currentTimeMillis(),
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurringPeriod: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transaction) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
} 