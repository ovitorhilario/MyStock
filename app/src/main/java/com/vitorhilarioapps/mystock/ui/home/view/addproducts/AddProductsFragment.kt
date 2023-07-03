package com.vitorhilarioapps.mystock.ui.home.view.addproducts

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentAddProductsBinding
import com.vitorhilarioapps.mystock.ui.home.viewmodel.FirestoreViewModel
import com.vitorhilarioapps.mystock.utils.getProductMap
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddProductsFragment : Fragment() {

    private var _binding: FragmentAddProductsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FirestoreViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentAddProductsBinding.inflate(inflater, group, false)
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
        // Setting Category Selection
        val itemsCategory = resources.getStringArray(R.array.category_list)
        val adapterCategory = ArrayAdapter(requireActivity(), R.layout.list_add_product_item, itemsCategory)
        binding.autoCompleteCategory.threshold = 1
        binding.autoCompleteCategory.setAdapter(adapterCategory)

        // Setting WeightType Selection
        val itemsWeightType = resources.getStringArray(R.array.weight_type_list)
        val adapterWeightType = ArrayAdapter(requireActivity(), R.layout.list_add_product_item, itemsWeightType)
        binding.autoCompleteWeight.setAdapter(adapterWeightType)
    }

    private fun setupClickListeners() {
        binding.btnAddProductsScanBarcode.setOnClickListener {
            scanBarcode()
        }

        binding.btnAddProducts.setOnClickListener {
            with(binding) {
                // Required
                val code = inputAddProductCode.text.intOrNull()
                val name = inputAddProductName.text.strOrNull()
                val purchasePrice = inputAddProductPurchasePrice.text.doubleOrNull()
                val salePrice = inputAddProductSalePrice.text.doubleOrNull()
                val amount = inputAddProductAmount.text.intOrNull()

                // Additional - can be null
                val description = inputAddProductDescription.text.strOrNull()
                val weight = inputAddProductWeight.text.doubleOrNull()
                val category = autoCompleteCategory.text.strOrNull()
                val weightType = autoCompleteWeight.text.strOrNull()

                val required = listOf(code, name, purchasePrice, salePrice, amount)

                if (required.all { it != null }) {
                    try {
                        val productMap = getProductMap(
                            code = code!!,
                            name = name!!,
                            amount = amount!!,
                            purchasePrice = purchasePrice!!,
                            salePrice = salePrice!!,
                            description = description ?: "",
                            category = category ?: "",
                            weight = weight ?: 0.0,
                            weightType = weightType ?: ""
                        )

                        addProduct(code, productMap)
                    } catch (e: IllegalArgumentException) {
                        requireActivity()
                            .showErrorToast(
                                error = getString(R.string.error_registering_product_text)
                            )
                    }
                } else {
                    requireActivity()
                        .showErrorToast(
                            error = getString(R.string.fill_in_all_required_information_text)
                        )
             }

            }
        }
    }

    /*-----------------
    |    Firestore    |
    -----------------*/

    private fun addProduct(code: Int, product: HashMap<String, Comparable<*>>) {
        lifecycleScope.launch {
            val hasProduct = viewModel.hasProduct(code)

            withContext(Dispatchers.Main) {
                if (!hasProduct) {
                    val successInAdd = viewModel.addProduct(code, product)

                    if (successInAdd) {
                        requireActivity()
                            .showSuccessToast(
                                message = resources.getString(R.string.product_added)
                            )
                    }

                    findNavController().navigateUp()
                } else {
                    requireActivity()
                        .showErrorToast(
                            error = resources.getString(R.string.error_to_add_product)
                        )
                }
            }
        }
    }

    private fun scanBarcode() {
        val scanner = GmsBarcodeScanning.getClient(requireActivity())

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue

                rawValue?.takeIf { it.isDigitsOnly() }?.let {
                    val number = rawValue.toLongOrNull()
                    number?.let {
                        binding.inputAddProductCode.setText(it.toString())
                    } ?: run {
                        requireActivity()
                            .showErrorToast(
                                error = getString(R.string.the_barcode_can_only_contain_numbers_text)
                            )
                    }
                }
            }
            .addOnFailureListener { e ->

                requireActivity()
                    .showErrorToast(
                        error = buildString { append(resources.getString(R.string.error), e.message.toString()) }
                    )
            }
    }

    private fun Editable?.strOrNull(): String? {
        val text = this.toString().trim()
        return text.ifEmpty { null }
    }

    private fun Editable?.intOrNull(): Int? {
        return this.toString().trim().toIntOrNull()
    }

    private fun Editable?.doubleOrNull(): Double? {
        return this.toString().trim().toDoubleOrNull()
    }
}