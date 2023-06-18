package com.vitorhilarioapps.mystock.ui.home.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.Timestamp
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentHomeBinding
import com.vitorhilarioapps.mystock.ui.home.view.home.adapter.HomeAdapter
import com.vitorhilarioapps.mystock.ui.home.view.home.model.HomeItems
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.data.model.Transaction
import com.vitorhilarioapps.mystock.ui.home.viewmodel.FirestoreViewModel
import com.vitorhilarioapps.mystock.utils.showInfoToast
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FirestoreViewModel by activityViewModels()
    private val currentPeriod = MutableLiveData<Period>(Period.AllHistory)

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initPeriod()
        getData()
        setupClickListeners()
    }

    private fun initPeriod() {
        currentPeriod.value = Period.AllHistory
    }

    /*-------------------------
    |    Fetch Remote Data    |
    -------------------------*/

    private fun getData() {
        lifecycleScope.launch {
            // Loading...
            showLoader()

            val products = viewModel.getProducts()
            val entrys = viewModel.getEntrys()
            val exits = viewModel.getExits()

            // Fetch data finished... OK!
            hideLoader()

            setupUI(
                products ?: emptyList(),
                entrys ?: emptyList(),
                exits ?: emptyList()
            )
        }
    }

    /*-----------------
    |    Handle UI    |
    -----------------*/

    private fun setupUI(
        products: List<Product>,
        entrys: List<Transaction.Entry>,
        exits: List<Transaction.Exit>
    ) {
        var stockValue = 0.0
        var productCont = 0

        products.forEach { product ->
            productCont += product.amount
            stockValue += product.amount * product.purchasePrice
        }

        var allExits = exits
        var allEntrys = entrys

        // Filtering by period

        val currentSeconds = Timestamp.now().seconds

        fun filterByPeriod(minLimit: Long) {
            allEntrys = entrys.filter { entry: Transaction.Entry ->
                entry.date.seconds in minLimit..currentSeconds
            }
            allExits = exits.filter { exit: Transaction.Exit ->
                exit.date.seconds in minLimit..currentSeconds
            }
        }

        when (currentPeriod.value ?: Period.AllHistory) {
            is Period.Last24Hours -> {
                val minLimitSeconds = currentSeconds - oneDayInSeconds
                filterByPeriod(minLimitSeconds)
            }
            is Period.LastWeek -> {
                val minLimitSeconds = currentSeconds - oneWeekInSeconds
                filterByPeriod(minLimitSeconds)
            }
            is Period.LastMonth -> {
                val minLimitSeconds = currentSeconds - oneMonthInSeconds
                filterByPeriod(minLimitSeconds)
            }
            is Period.AllHistory -> Unit
        }

        // Sums

        val sumExits = allExits.sumOf { it.resultPrice }
        val sumEntrys = allEntrys.sumOf { it.resultPrice }

        var profitOnSales = 0.0

        val allTransactions = (allEntrys + allExits)
            .sortedByDescending { transaction ->
                val dateInSeconds = when (transaction) {
                    is Transaction.Entry -> {
                        profitOnSales += transaction.profitOnSale
                        transaction.date.seconds
                    }

                    is Transaction.Exit -> {
                        transaction.date.seconds
                    }
                }

                dateInSeconds
            }

        val transactionBalance = sumEntrys - sumExits

        val listForAdapter = listOf(
            HomeItems.Info(profitOnSales, transactionBalance),
            HomeItems.ShortCut(stockValue, sumEntrys, sumExits, productCont),
            HomeItems.MainActions(""),
            HomeItems.RecentlyTransactions(allTransactions)
        )

        setupAdapter(listForAdapter, currentPeriod.value ?: Period.AllHistory, products.isNotEmpty())
    }

    private fun setupAdapter(listForAdapter: List<HomeItems>, period: Period, hasProducts: Boolean) {

        val adapter = HomeAdapter(
            data = listForAdapter,
            openShortCut = { id -> openShortCut(id) },
            openAllTransactions = { openAllTransactions() },
            setOnOrderPeriod = { p -> setOnOrderPeriod(p) },
            openAddProduct = { openAddProduct() },
            openAddPurchase = { openAddPurchase(hasProducts) },
            openAddSale = { openAddSale(hasProducts) },
            resources = resources,
            period = period
        )

        binding.rvHome.adapter = adapter
    }

    val openAddProduct = {
        val action = HomeFragmentDirections.actionHomeToProducts(true)
        findNavController().navigate(action)
    }

    val openAddPurchase = { hasProducts: Boolean  ->
        if (hasProducts) {
            val action = HomeFragmentDirections.actionHomeToTransactions(1, true)
            findNavController().navigate(action)
        } else {
            showToastEmptyStock()
        }
    }

    val openAddSale = { hasProducts: Boolean ->
        if (hasProducts) {
            val action = HomeFragmentDirections.actionHomeToTransactions(0, true)
            findNavController().navigate(action)
        } else {
            showToastEmptyStock()
        }
    }

    val openShortCut = { id : Int ->

        val destiny = when (id) {
            SHORT_CUT_TYPE.STOCK_VALUE.ID -> HomeFragmentDirections.actionHomeToProducts(false)
            SHORT_CUT_TYPE.AMOUNT_PRODUCT.ID -> HomeFragmentDirections.actionHomeToProducts(false)
            SHORT_CUT_TYPE.ENTRY_TRANSACTION.ID -> HomeFragmentDirections.actionHomeToTransactions(0)
            SHORT_CUT_TYPE.EXIT_TRANSACTION.ID -> HomeFragmentDirections.actionHomeToTransactions(1)
            else -> null
        }

        destiny?.let {
            findNavController()
                .navigate(it)
        }
    }

    val openNotifications = {
        // TODO for the next version
    }

    val openAllTransactions = {
        findNavController()
            .navigate(HomeFragmentDirections.actionHomeToTransactions(0))
    }

    val setOnOrderPeriod = { newPeriod : Period ->
        if (newPeriod != currentPeriod.value) {
            currentPeriod.value = newPeriod
            getData()
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefreshHome.setOnRefreshListener {
            getData()
        }
    }

    private fun showLoader() {
        binding.swipeRefreshHome.isRefreshing = true
    }

    private fun hideLoader() {
        binding.swipeRefreshHome.isRefreshing = false
    }

    private fun showToastEmptyStock() {
        requireActivity()
            .showInfoToast(
                info = resources.getString(R.string.dont_have_products)
            )
    }

    sealed class Period {
        object Last24Hours : Period()
        object LastWeek : Period()
        object LastMonth : Period()
        object AllHistory : Period()
    }

    companion object {
        const val TAG = "HomeFragment"
        const val oneDayInSeconds = 86400L
        const val oneWeekInSeconds = 604800L
        const val oneMonthInSeconds = 2592000L

        enum class SHORT_CUT_TYPE(val ID: Int) {
            STOCK_VALUE(0),
            AMOUNT_PRODUCT(1),
            ENTRY_TRANSACTION(2),
            EXIT_TRANSACTION(3)
        }
    }
}