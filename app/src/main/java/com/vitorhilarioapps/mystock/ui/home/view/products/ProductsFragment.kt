package com.vitorhilarioapps.mystock.ui.home.view.products

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentProductsBinding
import com.vitorhilarioapps.mystock.ui.home.view.products.adapter.ProductsAdapter
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.ui.home.view.products.model.ProductItem
import com.vitorhilarioapps.mystock.ui.home.viewmodel.FirestoreViewModel
import com.vitorhilarioapps.mystock.utils.moneyType
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductsFragment : Fragment() {

    private var _binding : FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private val viewModel : FirestoreViewModel by activityViewModels()

    //private val args: ProductsFragmentArgs by navArgs()
    private val selectedProducts: MutableList<Int> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentProductsBinding.inflate(inflater, group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
    }

    private fun initUI() {
        arguments?.let {
            val openAddProducts = ProductsFragmentArgs.fromBundle(it).openAddProducts
            arguments?.clear()

            if (openAddProducts) {
                addProducts()
            } else {
                getProducts()
                setupClickListeners()
            }
        } ?: run {
            getProducts()
            setupClickListeners()
        }
    }

    private fun getProducts() {
        // Loading...
        showLoader()

        lifecycleScope.launch {
            val products = viewModel.getProducts()

            withContext(Dispatchers.Main) {
                // Fetch data finished... OK!
                hideLoader()

                products?.takeIf { it.isNotEmpty() }?.let {
                    hideDataNotFound()
                    setupUI(it)
                } ?: run {
                    setupUI(emptyList())
                    showDataNotFound()
                }
            }
        }
    }

    private fun setupUI(products: List<Product>) {
        binding.tvProductsInfo.text = buildString {
            append(getString(R.string.registered_products))
            append(": ")
            append(products.size)
        }
        binding.tvProductsInfo2.text = buildString {
            append(getString(R.string.value_in_stock))
            append(": ")
            append(products.sumOf { it.purchasePrice * it.amount }.moneyType())
        }

        setupAdapter(products)
    }

    private fun setupAdapter(products: List<Product>) {
        binding.rvProducts.adapter = ProductsAdapter(
            data = products.map { ProductItem(it, false) },
            addProduct = { addProduct(it) },
            removeProduct = { removeProduct(it) },
            hasProduct = { hasProduct(it) },
            listSize = { selectedProducts.size },
            getResource = { getResource(it) }
        )
    }

    private fun setupClickListeners() {
        binding.fabProductsAdd.setOnClickListener {
            addProducts()
        }

        binding.swipeRefreshProducts.setOnRefreshListener {
            resetSelection()
            getProducts()
        }
    }

    private fun hideLoader() {
        binding.swipeRefreshProducts.isRefreshing = false
    }

    private fun showLoader() {
        binding.swipeRefreshProducts.isRefreshing = true
    }

    private fun showDataNotFound() {
        binding.containerProductsWithoutData.visibility = View.VISIBLE

        Picasso.get()
            .load("file:///android_asset/without_products_illustration.png")
            .into(binding.ivProductsWithoutData)

        binding.btnProductsAdd.setOnClickListener { addProducts() }
    }

    private fun hideDataNotFound() {
        binding.containerProductsWithoutData.visibility = View.GONE
    }

    /*--------------------------------
    |    Handle Selected Products    |
    --------------------------------*/

    private fun hasProduct(code: Int) = selectedProducts.contains(code)

    private fun addProduct(code: Int): Boolean {
        return if (!hasProduct(code)) {
            selectedProducts.add(code)
            listenDeleteActions()
            true
        } else false
    }

    private fun removeProduct(code: Int): Boolean {
        return if (hasProduct(code)) {
            selectedProducts.remove(code)
            listenDeleteActions()
            true
        } else false
    }

    private fun resetSelection() {
        selectedProducts.clear()
        hideDeleteActions()
        getProducts()
    }

    private fun listenDeleteActions() {
        if (selectedProducts.isNotEmpty()) {
            showDeleteActions()
        } else {
            hideDeleteActions()
        }
    }

    private fun hideDeleteActions() {
        binding.btnProductsCancel.visibility = View.GONE
        binding.btnProductsDelete.visibility = View.GONE
    }

    private fun showDeleteActions() {
        binding.btnProductsCancel.visibility = View.VISIBLE
        binding.btnProductsDelete.visibility = View.VISIBLE

        binding.btnProductsCancel.setOnClickListener {
            resetSelection()
        }

        binding.btnProductsDelete.setOnClickListener {
            delProducts(selectedProducts)
            resetSelection()
        }
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

    private fun addProducts() {
        val action = ProductsFragmentDirections.actionProductsToAddProducts()
        findNavController().navigate(action)
    }

    private fun delProducts(codes: List<Int>) {
        codes.takeIf { it.isNotEmpty() }?.let { products ->
            lifecycleScope.launch {
                val success = viewModel.delProducts(products)

                withContext(Dispatchers.Main) {
                    if (success) {
                        requireActivity()
                            .showSuccessToast(
                                message = resources.getString(R.string.success_in_delete)
                            )
                    } else {
                        requireActivity()
                            .showErrorToast(
                                error = getString(R.string.error_deleting_products_text)
                            )
                    }
                }
            }
        }
    }
}