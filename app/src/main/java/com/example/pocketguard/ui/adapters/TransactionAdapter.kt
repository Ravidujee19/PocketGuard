package com.example.pocketguard.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketguard.R
import com.example.pocketguard.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnMenu: ImageButton = view.findViewById(R.id.btnMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvAmount.text = String.format("$%.2f", transaction.amount)
        holder.tvAmount.setTextColor(
            holder.itemView.context.getColor(
                if (transaction.type == "INCOME") R.color.green else R.color.red
            )
        )
        holder.tvDescription.text = transaction.description
        holder.tvCategory.text = transaction.category
        holder.tvDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            .format(Date(transaction.date))

        // Set up menu button
        holder.btnMenu.setOnClickListener { view ->
            showPopupMenu(view, transaction)
        }
    }

    private fun showPopupMenu(view: View, transaction: Transaction) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.menu_transaction, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit -> {
                    onEditClick(transaction)
                    true
                }
                R.id.menu_delete -> {
                    onDeleteClick(transaction)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
} 