package com.vitorhilarioapps.mystock.ui.home.view.transactions.adapter

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.EntryTransactionItemBinding
import com.vitorhilarioapps.mystock.data.model.Transaction
import com.vitorhilarioapps.mystock.data.model.TransactionItem
import com.vitorhilarioapps.mystock.utils.moneyType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EntryAdapter(
    private val data: List<TransactionItem>,
    private val addTransaction: (Int) -> Boolean,
    private val removeTransaction: (Int) -> Boolean,
    private val hasTransaction: (Int) -> Boolean,
    private val listSize: () -> Int,
    private val resources: Resources
) : Adapter<EntryAdapter.EntryHolder>() {

    private var expandedPosition = -1;
    private var previousExpandedPosition = -1;

    inner class EntryHolder(binding: EntryTransactionItemBinding) : ViewHolder(binding.root) {
        private val layoutTransactionItem = binding.root
        private val ivEntryTransactionIcon = binding.ivEntryTransactionIcon
        private val tvEntryTransactionInfo = binding.tvEntryTransactionInfo
        private val tvEntryTransactionValue = binding.tvEntryTransactionValue
        private val tvEntryTransactionDate = binding.tvEntryTransactionDate
        private val btnEntryTransactionDropDown = binding.btnEntryTransactionDropDown
        private val tvEntryTransactionAllInfo = binding.tvEntryTransactionAllInfo
        private val containerTransactionEntryDropDrown = binding.containerTransactionEntryDropDrown

        private fun setSelected() {
            ivEntryTransactionIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_confirm))
            ivEntryTransactionIcon.setBackgroundDrawable(resources.getDrawable(R.drawable.short_cut_green_bg))
        }

        private fun setUnselected() {
            ivEntryTransactionIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_sale_price))
            ivEntryTransactionIcon.setBackgroundDrawable(resources.getDrawable(R.drawable.short_cut_blue_bg))
        }

        private fun setupClickListeners(transactionItem: TransactionItem, position: Int) {
            val transaction = transactionItem.data as Transaction.Entry

            btnEntryTransactionDropDown.setOnClickListener {
                val isExpanded = expandedPosition == position
                expandedPosition = if (isExpanded) -1 else position
                notifyItemChanged(position)
                notifyItemChanged(previousExpandedPosition)
            }

            layoutTransactionItem.setOnClickListener {
                if (listSize() >= 1) {
                    if (hasTransaction(transaction.id)) {
                        val success = removeTransaction(transaction.id)

                        if (success) {
                            setUnselected()
                            transactionItem.isSelected = false
                        }
                    } else {
                        val success = addTransaction(transaction.id)

                        if (success) {
                            setSelected()
                            transactionItem.isSelected = true
                        }
                    }
                }
            }

            layoutTransactionItem.setOnLongClickListener {
                if (listSize() == 0) {
                    val success = addTransaction(transaction.id)

                    if (success) {
                        setSelected()
                        transactionItem.isSelected = true
                    }
                }

                true
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
            tvEntryTransactionDate.text = buildString {
                append(
                    entry.date.toDate().getHours_()
                ); append(" | "); append(entry.date.toDate().getDay_())
            }
            tvEntryTransactionValue.text =
                buildString { append("+"); append(entry.resultPrice.moneyType()) }

            if (isExpanded) {
                previousExpandedPosition = position
                dropDownEnable()
            } else {
                dropDownDisable()
            }
        }
    }

    override fun onCreateViewHolder(group: ViewGroup, viewType: Int): EntryHolder {
        return EntryHolder(
            EntryTransactionItemBinding.inflate(LayoutInflater.from(group.context), group, false)
        )
    }

    override fun onBindViewHolder(holder: EntryHolder, position: Int) {
        holder.bindEntry(data[position], position)
    }

    override fun getItemCount(): Int = data.size


    fun Date.getDay_(): String {
        val dt = SimpleDateFormat("dd.MM.yyyy", Locale(""))
        return dt.format(this)
    }

    fun Date.getHours_(): String {
        val dt = SimpleDateFormat("HH:mm:ss", Locale(""))
        return dt.format(this)
    }
}