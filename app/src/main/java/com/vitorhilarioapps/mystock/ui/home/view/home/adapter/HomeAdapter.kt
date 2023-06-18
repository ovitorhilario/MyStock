package com.vitorhilarioapps.mystock.ui.home.view.home.adapter

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.chip.Chip
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.data.model.TransactionItem
import com.vitorhilarioapps.mystock.databinding.InfoItemBinding
import com.vitorhilarioapps.mystock.databinding.MainActionsItemBinding
import com.vitorhilarioapps.mystock.databinding.RecentlyTransactionsItemBinding
import com.vitorhilarioapps.mystock.databinding.ShortcutItemBinding
import com.vitorhilarioapps.mystock.databinding.ToolbarItemBinding
import com.vitorhilarioapps.mystock.ui.home.view.home.HomeFragment.*
import com.vitorhilarioapps.mystock.ui.home.view.home.model.HomeItems
import com.vitorhilarioapps.mystock.ui.home.view.transactions.adapter.TransactionAdapter
import com.vitorhilarioapps.mystock.utils.moneyType

class HomeAdapter(
    private val data: List<HomeItems>,
    private val openShortCut: (Int) -> Unit,
    private val openAllTransactions: () -> Unit,
    private val setOnOrderPeriod: (Period) -> Unit,
    private val openAddProduct: () -> Unit,
    private val openAddPurchase: () -> Unit,
    private val openAddSale: () -> Unit,
    private val resources: Resources,
    private val period: Period,
) : Adapter<ViewHolder>() {

    private var context: Context? = null

    inner class ToolBarHolder(binding: ToolbarItemBinding) : ViewHolder(binding.root) {
        private val tvToolbarName = binding.tvToolbarName
        private val ivToolbarPersonIcon = binding.ivToolbarPersonIcon

        fun bindToolBar(data: HomeItems.ToolBar) {
            tvToolbarName.text = buildString {
                append("Hi, ");
                append(data.name ?: "User")
            }

            data.imageUri?.let { uri ->
                Picasso.get()
                    .load(uri)
                    .error(R.drawable.user_avatar)
                    .into(ivToolbarPersonIcon)
            }
        }
    }

    inner class InfoHolder(binding: InfoItemBinding) : ViewHolder(binding.root) {
        private val tvInfoProfitOnSalesValue = binding.tvInfoProfitOnSalesValue
        private val chipGroupInfo = binding.chipGroupInfo
        private val tvInfoTransactionBalanceValue = binding.tvInfoTransactionBalanceValue
        private val ivInfoBanner = binding.ivInfoBanner

        private val chipInfoAllHistory = binding.chipInfoAllHistory
        private val chipInfoLastMonth = binding.chipInfoLastMonth
        private val chipInfoLastWeek = binding.chipInfoLastWeek
        private val chipInfoLastDay = binding.chipInfoLastDay

        fun bindInfo(data: HomeItems.Info) {
            tvInfoProfitOnSalesValue.text = data.profitOnSales.moneyType()
            tvInfoTransactionBalanceValue.text = data.transactionBalance.moneyType()

            Picasso.get()
                .load("file:///android_asset/stock_image.png")
                .into(ivInfoBanner)

            val chipToCheck: Chip = when (period) {
                Period.AllHistory -> chipInfoAllHistory
                Period.LastMonth -> chipInfoLastMonth
                Period.LastWeek -> chipInfoLastWeek
                Period.Last24Hours -> chipInfoLastDay
            }

            chipToCheck.isChecked = true

            chipGroupInfo.setOnCheckedStateChangeListener { group, _ ->
                val period = when (group.checkedChipId) {
                    chipInfoAllHistory.id -> Period.AllHistory
                    chipInfoLastMonth.id -> Period.LastMonth
                    chipInfoLastWeek.id -> Period.LastWeek
                    chipInfoLastDay.id -> Period.Last24Hours
                    else -> Period.AllHistory
                }

                setOnOrderPeriod(period)
            }
        }
    }

    inner class RecentlyTransactionsHolder(binding: RecentlyTransactionsItemBinding) :
        ViewHolder(binding.root) {
        private val rvRecentlyTransactions = binding.rvRecentlyTransactions
        private val tvRecentlyTransactionsShowAll = binding.tvRecentlyTransactionsShowAll
        private val showInEmptyData = binding.showInEmptyData
        private val btnNewTransaction = binding.btnNewTransaction

        fun bindRecentlyTransactions(data: HomeItems.RecentlyTransactions) {
            tvRecentlyTransactionsShowAll.setOnClickListener { openAllTransactions() }
            val tSize = data.transactions.size

            when (tSize) {
                0 -> {
                    showInEmptyData.visibility = View.VISIBLE
                    btnNewTransaction.setOnClickListener { openAddSale() }
                }

                in 1..4 -> {
                    rvRecentlyTransactions.adapter = TransactionAdapter(data.transactions.map { TransactionItem(it) })
                }

                in 5..Int.MAX_VALUE -> {
                    rvRecentlyTransactions.adapter =
                        TransactionAdapter(data.transactions.subList(0, 4).map { TransactionItem(it) })
                }
            }
        }
    }

    inner class ShortCutHolder(binding: ShortcutItemBinding) : ViewHolder(binding.root) {
        private val layoutShortCutItem1 = binding.layoutShortCutItem1
        private val tvShortcutValueItem1 = binding.tvShortcutValueItem1
        private val tvShortcutDescriptionItem1 = binding.tvShortcutDescriptionItem1
        private val ivShortcutIconItem1 = binding.ivShortcutIconItem1

        private val layoutShortCutItem2 = binding.layoutShortCutItem2
        private val tvShortcutValueItem2 = binding.tvShortcutValueItem2
        private val tvShortcutDescriptionItem2 = binding.tvShortcutDescriptionItem2
        private val ivShortcutIconItem2 = binding.ivShortcutIconItem2

        private val layoutShortCutItem3 = binding.layoutShortCutItem3
        private val tvShortcutValueItem3 = binding.tvShortcutValueItem3
        private val tvShortcutDescriptionItem3 = binding.tvShortcutDescriptionItem3
        private val ivShortcutIconItem3 = binding.ivShortcutIconItem3

        private val layoutShortCutItem4 = binding.layoutShortCutItem4
        private val tvShortcutValueItem4 = binding.tvShortcutValueItem4
        private val tvShortcutDescriptionItem4 = binding.tvShortcutDescriptionItem4
        private val ivShortcutIconItem4 = binding.ivShortcutIconItem4

        fun bindShortCutHolder(items: HomeItems.ShortCut) {

            tvShortcutDescriptionItem1.text = resources.getString(R.string.value_in_stock)
            tvShortcutValueItem1.text = items.stockValue.moneyType()
            ivShortcutIconItem1.setImageDrawable(resources.getDrawable(R.drawable.ic_money_simbol))
            ivShortcutIconItem1.background = resources.getDrawable(R.drawable.short_cut_yellow_bg)
            layoutShortCutItem1.setOnClickListener { openShortCut(Companion.SHORT_CUT_TYPE.STOCK_VALUE.ID) }

            tvShortcutDescriptionItem2.text = resources.getString(R.string.all_products)
            tvShortcutValueItem2.text = buildString { append(items.amountProducts) }
            ivShortcutIconItem2.setImageDrawable(resources.getDrawable(R.drawable.ic_info))
            ivShortcutIconItem2.background = resources.getDrawable(R.drawable.short_cut_blue_bg)
            layoutShortCutItem2.setOnClickListener { openShortCut(Companion.SHORT_CUT_TYPE.AMOUNT_PRODUCT.ID) }

            tvShortcutDescriptionItem3.text = resources.getString(R.string.entry)
            tvShortcutValueItem3.text = items.entryValue.moneyType()
            ivShortcutIconItem3.setImageDrawable(resources.getDrawable(R.drawable.ic_money_recive))
            ivShortcutIconItem3.background = resources.getDrawable(R.drawable.short_cut_green_bg)
            layoutShortCutItem3.setOnClickListener { openShortCut(Companion.SHORT_CUT_TYPE.ENTRY_TRANSACTION.ID) }

            tvShortcutDescriptionItem4.text = resources.getString(R.string.exit)
            tvShortcutValueItem4.text = items.exitValue.moneyType()
            ivShortcutIconItem4.setImageDrawable(resources.getDrawable(R.drawable.ic_money_send))
            ivShortcutIconItem4.background = resources.getDrawable(R.drawable.short_cut_red_bg)
            layoutShortCutItem4.setOnClickListener { openShortCut(Companion.SHORT_CUT_TYPE.EXIT_TRANSACTION.ID) }
        }
    }

    inner class MainActionsHolder(binding: MainActionsItemBinding) : ViewHolder(binding.root) {
        private val layoutMainActionAddProduct = binding.layoutMainActionAddProduct
        private val layoutMainActionAddPurchase = binding.layoutMainActionAddPurchase
        private val layoutMainActionAddSale = binding.layoutMainActionAddSale

        fun bindMainActions() {
            layoutMainActionAddProduct.setOnClickListener { openAddProduct() }
            layoutMainActionAddPurchase.setOnClickListener { openAddPurchase() }
            layoutMainActionAddSale.setOnClickListener { openAddSale() }
        }
    }

    override fun onCreateViewHolder(group: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(group.context)
        context = group.context

        return when (viewType) {
            ITEMS.TOOLBAR.id -> ToolBarHolder(ToolbarItemBinding.inflate(inflater, group, false))
            ITEMS.INFO.id -> InfoHolder(InfoItemBinding.inflate(inflater, group, false))
            ITEMS.RECENTLY_TRANSACTIONS.id -> RecentlyTransactionsHolder(
                RecentlyTransactionsItemBinding.inflate(inflater, group, false)
            )

            ITEMS.SHORT_CUT.id -> ShortCutHolder(
                ShortcutItemBinding.inflate(
                    inflater,
                    group,
                    false
                )
            )

            ITEMS.MAIN_ACTIONS.id -> MainActionsHolder(
                MainActionsItemBinding.inflate(
                    inflater,
                    group,
                    false
                )
            )

            else -> throw IllegalArgumentException("Error: Invalid View Holder")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ToolBarHolder -> holder.bindToolBar(data[position] as HomeItems.ToolBar)
            is InfoHolder -> holder.bindInfo(data[position] as HomeItems.Info)
            is RecentlyTransactionsHolder -> holder.bindRecentlyTransactions(data[position] as HomeItems.RecentlyTransactions)
            is ShortCutHolder -> holder.bindShortCutHolder(data[position] as HomeItems.ShortCut)
            is MainActionsHolder -> holder.bindMainActions()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is HomeItems.ToolBar -> ITEMS.TOOLBAR.id
            is HomeItems.Info -> ITEMS.INFO.id
            is HomeItems.RecentlyTransactions -> ITEMS.RECENTLY_TRANSACTIONS.id
            is HomeItems.ShortCut -> ITEMS.SHORT_CUT.id
            is HomeItems.MainActions -> ITEMS.MAIN_ACTIONS.id
        }
    }

    override fun getItemCount(): Int = data.size

    enum class ITEMS(val id: Int) {
        TOOLBAR(1),
        INFO(2),
        RECENTLY_TRANSACTIONS(3),
        SHORT_CUT(4),
        MAIN_ACTIONS(5)
    }
}