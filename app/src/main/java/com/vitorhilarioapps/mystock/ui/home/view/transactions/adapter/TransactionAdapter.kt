package com.vitorhilarioapps.mystock.ui.home.view.transactions.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.vitorhilarioapps.mystock.databinding.EntryTransactionItemBinding
import com.vitorhilarioapps.mystock.databinding.ExitTransactionItemBinding
import com.vitorhilarioapps.mystock.data.model.Transaction
import com.vitorhilarioapps.mystock.data.model.TransactionItem
import com.vitorhilarioapps.mystock.utils.moneyType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val data: List<TransactionItem>
) : Adapter<ViewHolder>() {

    private var expandedPosition = -1;
    private var previousExpandedPosition = -1;

    inner class EntryHolder(binding: EntryTransactionItemBinding) : ViewHolder(binding.root) {
        private val layoutTransactionItem = binding.root
        private val tvEntryTransactionInfo = binding.tvEntryTransactionInfo
        private val tvEntryTransactionValue = binding.tvEntryTransactionValue
        private val tvEntryTransactionDate = binding.tvEntryTransactionDate
        private val btnEntryTransactionDropDown = binding.btnEntryTransactionDropDown
        private val tvEntryTransactionAllInfo = binding.tvEntryTransactionAllInfo
        private val containerTransactionEntryDropDrown = binding.containerTransactionEntryDropDrown


        private fun setupClickListeners(transactionItem: TransactionItem, position: Int) {

            btnEntryTransactionDropDown.setOnClickListener {
                val isExpanded = expandedPosition == position
                expandedPosition = if (isExpanded) -1 else position
                notifyItemChanged(position)
                notifyItemChanged(previousExpandedPosition)
            }
        }

        private fun dropDownEnable() {
            containerTransactionEntryDropDrown.visibility = View.VISIBLE
        }

        private fun dropDownDisable() {
            containerTransactionEntryDropDrown.visibility = View.GONE
        }

        fun bindEntry(transactionItem: TransactionItem, position: Int) {
            val entry = transactionItem.data as Transaction.Entry
            val isExpanded = expandedPosition == position
            setupClickListeners(transactionItem, position)
            tvEntryTransactionInfo.text = entry.info.joinToString()
            tvEntryTransactionAllInfo.text = entry.info.joinToString(separator = "\n")
            tvEntryTransactionDate.text = buildString { append(entry.date.toDate().getHours_()); append(" | "); append(entry.date.toDate().getDay_()) }
            tvEntryTransactionValue.text = buildString { append("+"); append(entry.resultPrice.moneyType()) }

            if (isExpanded) {
                previousExpandedPosition = position
                dropDownEnable()
            } else {
                dropDownDisable()
            }
        }
    }

    inner class ExitHolder(binding: ExitTransactionItemBinding) : ViewHolder(binding.root) {
        private val layoutTransactionItem = binding.root
        private val tvExitTransactionInfo = binding.tvExitTransactionInfo
        private val tvExitTransactionValue = binding.tvExitTransactionValue
        private val tvExitTransactionDate = binding.tvExitTransactionDate
        private val btnExitTransactionDropDown = binding.btnExitTransactionDropDown
        private val tvExitTransactionAllInfo = binding.tvExitTransactionAllInfo
        private val containerTransactionExitDropDrown = binding.containerTransactionExitDropDrown

        private fun setupClickListeners(transactionItem: TransactionItem, position: Int) {
            val transaction = transactionItem.data as Transaction.Exit

            btnExitTransactionDropDown.setOnClickListener {
                val isExpanded = expandedPosition == position
                expandedPosition = if (isExpanded) -1 else position
                notifyItemChanged(position)
                notifyItemChanged(previousExpandedPosition)
            }
        }

        private fun dropDownEnable() {
            containerTransactionExitDropDrown.visibility = View.VISIBLE
        }

        private fun dropDownDisable() {
            containerTransactionExitDropDrown.visibility = View.GONE
        }

        fun bindExit(transactionItem: TransactionItem, position: Int) {
            val exit = transactionItem.data as Transaction.Exit
            val isExpanded = expandedPosition == position
            setupClickListeners(transactionItem, position)
            tvExitTransactionInfo.text = exit.info.joinToString()
            tvExitTransactionAllInfo.text = exit.info.joinToString(separator = "\n")
            tvExitTransactionDate.text = buildString { append(exit.date.toDate().getHours_()); append(" | "); append(exit.date.toDate().getDay_()) }
            tvExitTransactionValue.text = buildString { append("-"); append(exit.resultPrice.moneyType()) }

            if (isExpanded) {
                previousExpandedPosition = position
                dropDownEnable()
            } else {
                dropDownDisable()
            }
        }
    }

    override fun onCreateViewHolder(group: ViewGroup, viewType: Int): ViewHolder {
        return when(viewType) {
            TRANSACTION_TYPE.ENTRY.ID -> EntryHolder(
                EntryTransactionItemBinding.inflate(LayoutInflater.from(group.context), group, false)
            )
            TRANSACTION_TYPE.EXIT.ID -> ExitHolder(
                ExitTransactionItemBinding.inflate(LayoutInflater.from(group.context), group, false)
            )
            else -> throw IllegalArgumentException("Illegal Transaction View Holder")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(data[position].data) {
            is Transaction.Entry -> TRANSACTION_TYPE.ENTRY.ID
            is Transaction.Exit -> TRANSACTION_TYPE.EXIT.ID
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder) {
            is EntryHolder -> holder.bindEntry(data[position], position)
            is ExitHolder -> holder.bindExit(data[position], position)
        }
    }

    override fun getItemCount(): Int = data.size

    enum class TRANSACTION_TYPE(val ID: Int) {
        EXIT(0), ENTRY(1)
    }

    fun Date.getDay_() : String {
        val dt = SimpleDateFormat("dd.MM.yyyy", Locale(""))
        return dt.format(this)
    }

    fun Date.getHours_() : String {
        val dt = SimpleDateFormat("HH:mm:ss", Locale(""))
        return dt.format(this)
    }
}