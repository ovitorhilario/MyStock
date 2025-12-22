package com.vitorhilarioapps.mystock.ui.home.view.transactions.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
import androidx.core.view.isVisible

class EntryAdapter(
    private val data: List<TransactionItem>,
    private val addTransaction: (Int) -> Boolean,
    private val removeTransaction: (Int) -> Boolean,
    private val hasTransaction: (Int) -> Boolean,
    private val listSize: () -> Int,
    private val getResource: (Int) -> Drawable?
) : Adapter<EntryAdapter.EntryHolder>() {

    inner class EntryHolder(binding: EntryTransactionItemBinding) : ViewHolder(binding.root) {
        private val layoutTransactionItem = binding.root
        private val ivEntryTransactionIcon = binding.ivEntryTransactionIcon
        private val tvEntryTransactionInfo = binding.tvEntryTransactionInfo
        private val tvEntryTransactionValue = binding.tvEntryTransactionValue
        private val tvEntryTransactionDate = binding.tvEntryTransactionDate
        private val btnEntryTransactionDropDown = binding.btnEntryTransactionDropDown
        private val tvEntryTransactionAllInfo = binding.tvEntryTransactionAllInfo
        private val containerTransactionEntryDropDrown = binding.containerTransactionEntryDropDrown

        private var currentTransactionItem: TransactionItem? = null

        init {
            btnEntryTransactionDropDown.setOnClickListener {
                if (containerTransactionEntryDropDrown.isVisible) {
                    containerTransactionEntryDropDrown.visibility = View.GONE
                } else {
                    containerTransactionEntryDropDrown.visibility = View.VISIBLE
                }
            }

            layoutTransactionItem.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    currentTransactionItem?.let { transactionItem ->
                        val transaction = transactionItem.data as Transaction.Entry
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
                }
            }

            layoutTransactionItem.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    currentTransactionItem?.let { transactionItem ->
                        val transaction = transactionItem.data as Transaction.Entry
                        if (listSize() == 0) {
                            val success = addTransaction(transaction.id)

                            if (success) {
                                setSelected()
                                transactionItem.isSelected = true
                            }
                        }
                    }
                }
                true
            }
        }

        private fun setSelected() {
            ivEntryTransactionIcon.setImageDrawable(getResource(R.drawable.ic_confirm))
            ivEntryTransactionIcon.setBackgroundDrawable(getResource(R.drawable.short_cut_green_bg))
        }

        private fun setUnselected() {
            ivEntryTransactionIcon.setImageDrawable(getResource(R.drawable.ic_sale_price))
            ivEntryTransactionIcon.setBackgroundDrawable(getResource(R.drawable.short_cut_blue_bg))
        }


        fun bindEntry(transactionItem: TransactionItem, position: Int) {
            currentTransactionItem = transactionItem
            val entry = transactionItem.data as Transaction.Entry

            if (hasTransaction(entry.id)) {
                setSelected()
            } else {
                setUnselected()
            }

            tvEntryTransactionInfo.text = entry.info.joinToString()
            tvEntryTransactionAllInfo.text = entry.info.joinToString(separator = "\n")
            tvEntryTransactionDate.text = buildString {
                append(
                    entry.date.toDate().getHours_()
                ); append(" | "); append(entry.date.toDate().getDay_())
            }
            tvEntryTransactionValue.text =
                buildString { append("+"); append(entry.resultPrice.moneyType()) }

            // Reset dropdown state on bind
            containerTransactionEntryDropDrown.visibility = View.GONE
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