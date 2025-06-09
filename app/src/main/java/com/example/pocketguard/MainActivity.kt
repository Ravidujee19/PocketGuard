package com.example.pocketguard

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.pocketguard.ui.BudgetFragment
import com.example.pocketguard.ui.FragmentCallback
import com.example.pocketguard.ui.SettingsFragment
import com.example.pocketguard.ui.TransactionsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), FragmentCallback {
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNavigation()
        if (savedInstanceState == null) {
            loadFragment(TransactionsFragment())
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_transactions -> {
                    loadFragment(TransactionsFragment())
                    true
                }
                R.id.navigation_budget -> {
                    loadFragment(BudgetFragment())
                    true
                }
                R.id.navigation_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val tag = when (fragment) {
            is BudgetFragment -> "BudgetFragment"
            is TransactionsFragment -> "TransactionsFragment"
            is SettingsFragment -> "SettingsFragment"
            else -> fragment.javaClass.simpleName
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }

    override fun onTransactionChanged() {
        val budgetFragment = supportFragmentManager.findFragmentByTag("BudgetFragment") as? BudgetFragment
        budgetFragment?.refreshBudgetData()
    }
}