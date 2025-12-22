package com.vitorhilarioapps.mystock.ui.home.view.transactions.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.data.model.Transaction
import com.vitorhilarioapps.mystock.data.model.TransactionItem
import com.vitorhilarioapps.mystock.databinding.ExitTransactionItemBinding
import com.vitorhilarioapps.mystock.utils.moneyType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExitAdapter(
    private val data: List<TransactionItem>,
    private val addTransaction: (Int) -> Boolean,
    private val removeTransaction: (Int) -> Boolean,
    private val hasTransaction: (Int) -> Boolean,
    private val listSize: () -> Int,
    private val getResource: (Int) -> Drawable?
) : RecyclerView.Adapter<ExitAdapter.ExitHolder>() {


    inner class ExitHolder(binding: ExitTransactionItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val layoutTransactionItem = binding.root
        private val ivExitTransactionIcon = binding.ivExitTransactionIcon
        private val tvExitTransactionInfo = binding.tvExitTransactionInfo
        private val tvExitTransactionValue = binding.tvExitTransactionValue
        private val tvExitTransactionDate = binding.tvExitTransactionDate
        private val btnExitTransactionDropDown = binding.btnExitTransactionDropDown
        private val tvExitTransactionAllInfo = binding.tvExitTransactionAllInfo
        private val containerTransactionExitDropDrown = binding.containerTransactionExitDropDrown

        private var currentTransactionItem: TransactionItem? = null

        init {
            btnExitTransactionDropDown.setOnClickListener {
                if (containerTransactionExitDropDrown.visibility == View.VISIBLE) {
                    containerTransactionExitDropDrown.visibility = View.GONE
                } else {
                    containerTransactionExitDropDrown.visibility = View.VISIBLE
                }
            }

            layoutTransactionItem.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    currentTransactionItem?.let { transactionItem ->
                        val transaction = transactionItem.data as Transaction.Exit
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
                        val transaction = transactionItem.data as Transaction.Exit
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
            ivExitTransactionIcon.setImageDrawable(getResource(R.drawable.ic_confirm))
            ivExitTransactionIcon.setBackgroundDrawable(getResource(R.drawable.short_cut_green_bg))
        }

        private fun setUnselected() {
            ivExitTransactionIcon.setImageDrawable(getResource(R.drawable.ic_purchase))
            ivExitTransactionIcon.setBackgroundDrawable(getResource(R.drawable.short_cut_red_bg))
        }

        fun bindExit(transactionItem: TransactionItem, position: Int) {
            currentTransactionItem = transactionItem
            val exit = transactionItem.data as Transaction.Exit

            if (hasTransaction(exit.id)) {
                setSelected()
            } else {
                setUnselected()
            }

            tvExitTransactionInfo.text = exit.info.joinToString()
            tvExitTransactionAllInfo.text = exit.info.joinToString(separator = "\n")
            tvExitTransactionDate.text = buildString {
                append(
                    exit.date.toDate().getHours_()
                ); append(" | "); append(exit.date.toDate().getDay_())
            }
            tvExitTransactionValue.text =
                buildString { append("-"); append(exit.resultPrice.moneyType()) }

            // Reset dropdown state on bind
            containerTransactionExitDropDrown.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(group: ViewGroup, viewType: Int): ExitHolder {
        return ExitHolder(
            ExitTransactionItemBinding.inflate(LayoutInflater.from(group.context), group, false)
        )
    }

    override fun onBindViewHolder(holder: ExitHolder, position: Int) {
        holder.bindExit(data[position], position)
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