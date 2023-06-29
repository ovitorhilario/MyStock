package com.vitorhilarioapps.mystock.ui.home.view.transactions

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentNestedTransactionBinding
import com.vitorhilarioapps.mystock.data.model.Transaction
import com.vitorhilarioapps.mystock.data.model.TransactionItem
import com.vitorhilarioapps.mystock.ui.home.view.transactions.adapter.EntryAdapter
import com.vitorhilarioapps.mystock.ui.home.view.transactions.adapter.ExitAdapter
import com.vitorhilarioapps.mystock.ui.home.view.transactions.model.TransactionType
import com.vitorhilarioapps.mystock.ui.home.viewmodel.FirestoreViewModel
import com.vitorhilarioapps.mystock.utils.showInfoToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NestedTransactionFragment : Fragment() {

    private var _binding : FragmentNestedTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel : FirestoreViewModel by activityViewModels()
    private val currentState = MutableLiveData<TransactionType>()

    private val selectedExits: MutableList<Int> = mutableListOf()
    private val selectedEntrys: MutableList<Int> = mutableListOf()


    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentNestedTransactionBinding.inflate(inflater, group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getState()
        setupUI()
        setupClickListeners()
    }

    /*---------------------
    |    Getting State    |
    ---------------------*/

    private fun getState() = when (arguments?.getInt(POSITION_KEY)) {
        0 -> currentState.value = TransactionType.Sale
        1 -> currentState.value = TransactionType.Purchase
        else -> currentState.value = TransactionType.Sale
    }

    /*-----------------
    |    Handle UI    |
    -----------------*/

    private fun setupUI() {
        hideDeleteButtons()
        selectedEntrys.clear()
        selectedExits.clear()

        when (currentState.value as TransactionType) {
            is TransactionType.Sale -> {
                binding.fabNewTransaction.setImageResource(R.drawable.ic_sale_add)
                getSaleTransitions()
            }
            is TransactionType.Purchase -> {
                binding.fabNewTransaction.setImageResource(R.drawable.ic_purchase_add)
                getPurchaseTransitions()
            }
        }
    }

    private fun setupEmptyUI() {
        setupAdapter(emptyList())
        showNothingFound()
    }

    private fun setupAdapter(transactions: List<Transaction>) {
        binding.swipeRefreshTransaction.isRefreshing = false

        val adapter = when (currentState.value as TransactionType) {
            is TransactionType.Sale -> EntryAdapter(
                data = transactions.map { TransactionItem(it, false) },
                addTransaction = {
                    val success = selectedEntrys.add(it)
                    listenSelectChanges()
                    success
                 },
                removeTransaction = {
                    val success = selectedEntrys.remove(it)
                    listenSelectChanges()
                    success
                },
                hasTransaction = { selectedEntrys.contains(it) },
                listSize = { selectedEntrys.size },
                getResource = { getResource(it) }
            )
            is TransactionType.Purchase -> ExitAdapter(
                data = transactions.map { TransactionItem(it, false) },
                addTransaction = {
                    val success = selectedExits.add(it)
                    listenSelectChanges()
                    success
                 },
                removeTransaction = {
                    val success = selectedExits.remove(it)
                    listenSelectChanges()
                    success
                },
                hasTransaction = { selectedExits.contains(it) },
                listSize = { selectedExits.size },
                getResource = { getResource(it) }
            )
        }

        binding.rvNestedTransaction.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.swipeRefreshTransaction.setOnRefreshListener {
            cancelSelection()
            setupUI()
        }
        binding.fabNewTransaction.setOnClickListener { getProductIds() }
    }

    private fun showNothingFound() {
        binding.containerTransactionWithoutData.visibility = View.VISIBLE

        when (currentState.value as TransactionType) {
            is TransactionType.Sale -> {

                Picasso.get()
                    .load("file:///android_asset/sale_illustration.png")
                    .into(binding.ivTransactionWithoutData)

                binding.btnTransactionWithoutDataAdd.setOnClickListener {
                    getProductIds()
                }
            }
            is TransactionType.Purchase -> {

                Picasso.get()
                    .load("file:///android_asset/purchase_illustration.png")
                    .into(binding.ivTransactionWithoutData)

                binding.btnTransactionWithoutDataAdd.setOnClickListener {
                    getProductIds()
                }
            }
        }
    }

    private fun showLoader() {
        binding.swipeRefreshTransaction.isRefreshing = true
    }

    private fun hideLoader() {
        binding.swipeRefreshTransaction.isRefreshing = false
    }

    /*--------------------------------
    |    Handle Selected Products    |
    --------------------------------*/

    private fun listenSelectChanges() {
        if (selectedEntrys.isNotEmpty() || selectedExits.isNotEmpty()) {
            showDeleteButtons()
        } else {
            hideDeleteButtons()
            setupUI()
        }
    }

    private fun showDeleteButtons() {
        binding.fabDeleteTransactions.visibility = View.VISIBLE
        binding.fabCancelDelete.visibility = View.VISIBLE

        binding.fabCancelDelete.setOnClickListener {
            cancelSelection()
        }

        binding.fabDeleteTransactions.setOnClickListener {
            when (currentState.value as TransactionType) {
                is TransactionType.Sale -> {
                    selectedEntrys.takeIf { it.isNotEmpty() }?.let { entrys ->
                        delEntrys(entrys)
                    }
                }
                is TransactionType.Purchase -> {
                    selectedExits.takeIf { it.isNotEmpty() }?.let { exits ->
                        delExits(exits)
                    }
                }
            }

            cancelSelection()
        }
    }

    private fun cancelSelection() {
        selectedEntrys.clear()
        selectedExits.clear()
        listenSelectChanges()
    }

    private fun hideDeleteButtons() {
        binding.fabDeleteTransactions.visibility = View.GONE
        binding.fabCancelDelete.visibility = View.GONE
    }

    private fun getResource(id: Int): Drawable? {
        return try {
            ContextCompat.getDrawable(requireActivity(), id)
        } catch (e: Exception) {
            ContextCompat.getDrawable(requireActivity(), R.drawable.short_cut_red_bg)
        }
    }

    /*-----------------
    |    Firestore    |
    -----------------*/

    private fun getPurchaseTransitions() {
        showLoader()
        lifecycleScope.launch {
            val exits = viewModel.getExits()

            withContext(Dispatchers.Main) {
                hideLoader()

                exits?.let {
                    setupAdapter(it.sortedByDescending { exit -> exit.date.seconds })
                } ?: setupEmptyUI()
            }
        }
    }

    private fun getSaleTransitions() {
        showLoader()
        lifecycleScope.launch {
            val entrys = viewModel.getEntrys()

            withContext(Dispatchers.Main) {
                hideLoader()

                entrys?.let {
                    setupAdapter(it.sortedByDescending { entry -> entry.date.seconds })
                } ?: setupEmptyUI()
            }
        }
    }

    private fun getProductIds() {
        lifecycleScope.launch {
            val hasProducts = viewModel.getProducts()

            withContext(Dispatchers.Main) {
                hasProducts?.takeIf { it.isNotEmpty() }?.let {
                    val action = TransactionsFragmentDirections.actionTransactionsToSelectProducts(currentState.value ?: TransactionType.Sale)
                    findNavController().navigate(action)
                } ?: run {
                    requireActivity()
                        .showInfoToast(info = getString(R.string.dont_have_products))
                }
            }
        }
    }

    private fun delExits(ids: List<Int>) {
        lifecycleScope.launch {
            val success = viewModel.delExits(ids)

            withContext(Dispatchers.Main) {
                if (success) {
                    requireActivity()
                        .showSuccessToast(
                            message = getString(R.string.success_in_delete_transaction_text)
                        )
                } else {
                    requireActivity()
                        .showSuccessToast(
                            message = getString(R.string.error_deleting_transaction_text)
                        )
                }
            }
        }
    }

    private fun delEntrys(ids: List<Int>) {
        lifecycleScope.launch {
            val success = viewModel.delEntrys(ids)

            withContext(Dispatchers.Main) {
                if (success) {
                    requireActivity()
                        .showSuccessToast(
                            message = getString(R.string.success_in_delete_transaction_text)
                        )
                } else {
                    requireActivity()
                        .showSuccessToast(
                            message = getString(R.string.error_deleting_transaction_text)
                        )
                }
            }
        }
    }

    companion object {
        const val POSITION_KEY = "POSITION_KEY"

        fun getInstance(pos: Int) : Fragment =
            NestedTransactionFragment().apply {
                arguments = Bundle().apply {
                    putInt(POSITION_KEY, pos)
                }
            }
    }
}
