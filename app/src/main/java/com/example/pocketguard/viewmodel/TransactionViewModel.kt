package com.example.pocketguard.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
import com.example.pocketguard.data.SharedPreferencesManager
import com.example.pocketguard.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class TransactionViewModel(private val sharedPreferencesManager: SharedPreferencesManager) : ViewModel() {
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
//        viewModelScope.launch {
            try {
                _transactions.value = sharedPreferencesManager.getTransactions()
            } catch (e: Exception) {
                _error.value = e.message
            }
//        }
    }

    fun addTransaction(transaction: Transaction) {
//        viewModelScope.launch {
            try {
                sharedPreferencesManager.addTransaction(transaction)
                loadTransactions() // Reload transactions after adding
            } catch (e: Exception) {
                _error.value = e.message
            }
//        }
    }

    fun updateTransaction(transaction: Transaction) {
//        viewModelScope.launch {
            try {
                sharedPreferencesManager.updateTransaction(transaction)
                loadTransactions() // Reload transactions after updating
            } catch (e: Exception) {
                _error.value = e.message
            }
//        }
    }

    fun deleteTransaction(transaction: Transaction) {
//        viewModelScope.launch {
            try {
                sharedPreferencesManager.deleteTransaction(transaction)
                loadTransactions() // Reload transactions after deleting
            } catch (e: Exception) {
                _error.value = e.message
            }
//        }
    }
} 