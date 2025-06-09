package com.example.pocketguard.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.pocketguard.R
import com.example.pocketguard.data.SharedPreferencesManager
import com.example.pocketguard.model.Transaction
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.util.*

class DashboardFragment : Fragment() {
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private lateinit var tvBalanceAmount: TextView
    private lateinit var tvIncomeAmount: TextView
    private lateinit var tvExpenseAmount: TextView
    private lateinit var pieChart: PieChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferencesManager = SharedPreferencesManager(requireContext())
        
        // Initialize views
        tvBalanceAmount = view.findViewById(R.id.tvBalanceAmount)
        tvIncomeAmount = view.findViewById(R.id.tvIncomeAmount)
        tvExpenseAmount = view.findViewById(R.id.tvExpenseAmount)
        pieChart = view.findViewById(R.id.pieChart)

        setupPieChart()
        updateDashboard()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.isEnabled = true
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
        }
    }

    private fun updateDashboard() {
        val transactions = sharedPreferencesManager.getTransactions()
        
        // Calculate totals
        val totalIncome = transactions
            .filter { it.type == "INCOME" }
            .sumOf { it.amount }

        val totalExpenses = transactions
            .filter { it.type == "EXPENSE" }
            .sumOf { it.amount }

        val balance = totalIncome - totalExpenses

        // Format currency
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
        
        // Update UI with proper formatting
        tvBalanceAmount.text = currencyFormat.format(balance)
        tvIncomeAmount.text = currencyFormat.format(totalIncome)
        tvExpenseAmount.text = currencyFormat.format(totalExpenses)

        // Set text colors
        tvBalanceAmount.setTextColor(
            requireContext().getColor(
                if (balance >= 0) R.color.green else R.color.red
            )
        )

        // Update pie chart
        updatePieChart(transactions)
    }

    private fun updatePieChart(transactions: List<Transaction>) {
        val expenseTransactions = transactions.filter { it.type == "EXPENSE" }
        if (expenseTransactions.isEmpty()) {
            pieChart.clear()
            pieChart.invalidate()
            return
        }

        // Group transactions by category
        val categoryTotals = expenseTransactions.groupBy { it.category }
            .mapValues { it.value.sumOf { transaction -> transaction.amount } }

        // Create pie entries
        val entries = categoryTotals.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "Categories").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueFormatter = PercentFormatter(pieChart)
            valueTextColor = Color.WHITE
        }

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }
} 