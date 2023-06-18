package com.vitorhilarioapps.mystock.ui.home.view.registers.entry

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
import com.vitorhilarioapps.mystock.databinding.FragmentRegisterEntryBinding
import com.vitorhilarioapps.mystock.ui.home.view.registers.entry.adapter.EntryAdapter
import com.vitorhilarioapps.mystock.ui.home.viewmodel.FirestoreViewModel
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import kotlinx.coroutines.launch

class RegisterEntryFragment : Fragment() {

    private var _binding : FragmentRegisterEntryBinding? = null
    private val binding get() = _binding!!
    private val viewModel : FirestoreViewModel by activityViewModels()

    private val args by navArgs<RegisterEntryFragmentArgs>()

    private var selectionProductMap: HashMap<Int, Int> = hashMapOf()
    private var selectedProducts: List<Product> = listOf()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentRegisterEntryBinding.inflate(inflater, group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getProducts()
        setupClickListeners()
    }

    /*-------------------------
    |    Fetch Remote Data    |
    -------------------------*/

    fun getProducts() {
        lifecycleScope.launch {
            val products = viewModel.getProducts(args.selectedProducts.toList())

            products?.let {
                setupAdapter(it)
            } ?: Unit
        }
    }

    /*-----------------
    |    Handle UI    |
    -----------------*/

    fun setupClickListeners() {

        binding.btnEntryConfirm.setOnClickListener {
            val products = selectionProductMap
                .filter { (_, cont) -> cont > 0 }.toList()

            if (products.isNotEmpty()) {
                registerEntry(products)
            }
        }
    }

    fun setupAdapter(products: List<Product>) {
        selectedProducts = products

        // initialize with 0 amount for each product
        products.forEach{
            selectionProductMap[it.code] = 0
        }

        binding.rvEntry.adapter = EntryAdapter(
            data = products,
            addProductCont = { code ->
                val currentProductCont = selectionProductMap[code] ?: 0
                val addedProductCont = currentProductCont + 1

                products.firstOrNull { it.code == code }?.let { product ->
                    if (addedProductCont <= product.amount) {
                        selectionProductMap[code] = (currentProductCont + 1)
                    }
                }

                selectionProductMap[code] ?: 0
            },
            removeProductCont = { code ->
                val currentProductCont = selectionProductMap[code] ?: 0
                val removedProductCont = currentProductCont - 1

                products.firstOrNull { it.code == code }?.let { product ->
                    if (removedProductCont >= 0) {
                        selectionProductMap[code] = (currentProductCont - 1)
                    }
                }

                selectionProductMap[code] ?: 0
            },
            resources = resources
        )
    }

    /*-----------------
    |    Firestore    |
    -----------------*/

    private fun registerEntry(products: List<Pair<Int, Int>>) {
        lifecycleScope.launch {
            val currentProducts = viewModel.getProducts(args.selectedProducts.toList())

            currentProducts?.let {
                currentProducts.filter { it.amount > 0 }.takeIf { it.isNotEmpty() }?.let { validProducts ->
                    val isSuccessful = viewModel.addEntryTransaction(products, validProducts)

                    if (isSuccessful) {
                        requireActivity()
                            .showSuccessToast(
                                message = resources.getString(R.string.success_in_add_transaction)
                            )

                            findNavController().navigate(
                                RegisterEntryFragmentDirections.actionRegisterEntryToTransactions(0)
                            )
                    } else {
                        requireActivity()
                            .showErrorToast(
                                error = resources.getString(R.string.error_to_add_transaction)
                            )
                    }
                }
            }

            getProducts()
        }
    }
}