package com.example.pocketguard.model

data class BudgetCategory(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val amount: Double,
    val spent: Double = 0.0,
    val month: Int,
    val year: Int
) 