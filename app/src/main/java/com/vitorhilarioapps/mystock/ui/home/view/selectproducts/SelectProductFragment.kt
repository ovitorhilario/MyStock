package com.vitorhilarioapps.mystock.ui.home.view.selectproducts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.databinding.FragmentSelectProductsBinding
import com.vitorhilarioapps.mystock.ui.home.view.products.model.ProductItem
import com.vitorhilarioapps.mystock.ui.home.viewmodel.FirestoreViewModel
import com.vitorhilarioapps.mystock.ui.home.view.selectproducts.adapter.SelectProductsAdapter
import com.vitorhilarioapps.mystock.ui.home.view.transactions.model.TransactionType
import com.vitorhilarioapps.mystock.utils.showInfoToast
import kotlinx.coroutines.launch

class SelectProductFragment : Fragment() {

    private var _binding: FragmentSelectProductsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FirestoreViewModel by activityViewModels()
    private val args by navArgs<SelectProductFragmentArgs>()

    private val selectedItems: MutableList<Int> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSelectProductsBinding.inflate(inflater, group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
    }

    /*-----------------
    |    Handle UI    |
    -----------------*/

    private fun setupUI() {
        lifecycleScope.launch {
            val products = viewModel.getProducts()

            products?.let {
                if (it.isNotEmpty()) {
                    setupAdapter(it, args.transactionType)
                } else {
                    findNavController().navigateUp()
                }
            } ?: findNavController().navigateUp()
        }
    }

    private fun setupClickListeners() {

        binding.fabSelectProductsOk.setOnClickListener {
            if (selectedItems.isNotEmpty()) {
                val action = when (args.transactionType as TransactionType) {
                    is TransactionType.Sale -> SelectProductFragmentDirections
                        .actionFragmentSelectProductsToFragmentRegisterEntry(selectedItems.toIntArray())
                    is TransactionType.Purchase -> SelectProductFragmentDirections
                        .actionFragmentSelectProductsToFragmentRegisterExit(selectedItems.toIntArray())
                }

                findNavController().navigate(action)
            } else {
                requireActivity()
                    .showInfoToast(
                        info = resources.getString(R.string.select_at_least_one)
                    )
            }
        }
    }

    private fun setupAdapter(list: List<Product>, transaction: TransactionType) {
        setupClickListeners()

        binding.rvSelectProducts.adapter = SelectProductsAdapter(
            transactionType = transaction,
            listData = list.map { ProductItem(it, false) },
            addItem = { addItem(it) },
            deleteItem = { deleteItem(it) },
            resources = resources
        )
    }

    /*-----------------
    |    Firestore    |
    -----------------*/

    private fun addItem(code: Int): Boolean {
        return if (!selectedItems.contains(code)) {
            selectedItems.add(code)
            true
        } else false
    }

    private fun deleteItem(code: Int): Boolean {
        return if (selectedItems.contains(code)) {
            selectedItems.remove(code)
            true
        } else false
    }
}