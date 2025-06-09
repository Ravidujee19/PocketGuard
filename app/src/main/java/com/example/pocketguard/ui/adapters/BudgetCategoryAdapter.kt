package com.example.pocketguard.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketguard.R
import com.example.pocketguard.model.BudgetCategory
import com.google.android.material.progressindicator.LinearProgressIndicator

class BudgetCategoryAdapter(
    private var categories: List<BudgetCategory>,
    private val onEditClick: (BudgetCategory) -> Unit,
    private val onDeleteClick: (BudgetCategory) -> Unit
) : RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvBudgetAmount: TextView = view.findViewById(R.id.tvBudgetAmount)
        val tvSpentAmount: TextView = view.findViewById(R.id.tvSpentAmount)
        val progressCategory: LinearProgressIndicator = view.findViewById(R.id.progressCategory)
        val btnEdit: TextView = view.findViewById(R.id.btnEdit)
        val btnDelete: TextView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        
        holder.tvCategoryName.text = category.name
        holder.tvBudgetAmount.text = "Budget: ${category.amount}"
        holder.tvSpentAmount.text = "Spent: ${category.spent}"

        // Calculate progress
        val progress = if (category.amount > 0) {
            (category.spent / category.amount * 100).toInt()
        } else {
            0
        }
        holder.progressCategory.progress = progress

        // Set progress color based on percentage
        val color = when {
            progress >= 100 -> R.color.red
            progress >= 80 -> R.color.orange
            else -> R.color.green
        }
        holder.progressCategory.setIndicatorColor(
            ContextCompat.getColor(holder.itemView.context, color)
        )

        holder.btnEdit.setOnClickListener { onEditClick(category) }
        holder.btnDelete.setOnClickListener { onDeleteClick(category) }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<BudgetCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }
} 