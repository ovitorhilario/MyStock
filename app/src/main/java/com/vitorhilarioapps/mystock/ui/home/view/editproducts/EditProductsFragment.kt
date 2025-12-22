package com.vitorhilarioapps.mystock.ui.home.view.editproducts

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentEditProductsBinding
import com.vitorhilarioapps.mystock.ui.home.viewmodel.FirestoreViewModel
import com.vitorhilarioapps.mystock.utils.getProductMap
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProductsFragment : Fragment() {

    private var _binding: FragmentEditProductsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FirestoreViewModel by activityViewModels()
    private val args: EditProductsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentEditProductsBinding.inflate(inflater, group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadProductData()
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

    private fun loadProductData() {
        val productCode = args.productCode

        lifecycleScope.launch {
            val product = viewModel.getProduct(productCode)

            withContext(Dispatchers.Main) {
                product?.let {
                    with(binding) {
                        inputEditProductCode.setText(it.code)
                        inputEditProductName.setText(it.name)
                        inputEditProductAmount.setText(it.amount.toString())
                        inputEditProductPurchasePrice.setText(it.purchasePrice.toString())
                        inputEditProductSalePrice.setText(it.salePrice.toString())
                        inputEditProductDescription.setText(it.description)
                        inputEditProductWeight.setText(if (it.weight != 0.0) it.weight.toString() else "")
                        autoCompleteCategory.setText(it.category, false)
                        autoCompleteWeight.setText(it.weightType, false)
                    }
                } ?: run {
                    requireActivity().showErrorToast(
                        error = getString(R.string.error_loading_product)
                    )
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun setupClickListeners() {

        binding.btnEditProducts.setOnClickListener {
            if (validateInputs()) {
                with(binding) {

                    val code = inputEditProductCode.text.strOrNull() ?: ""
                    val name = inputEditProductName.text.strOrNull() ?: ""
                    val purchasePrice = inputEditProductPurchasePrice.text.doubleOrNull() ?: 0.0
                    val salePrice = inputEditProductSalePrice.text.doubleOrNull() ?: 0.0
                    val amount = inputEditProductAmount.text.intOrNull() ?: 0

                    // Additional - can be null
                    val description = inputEditProductDescription.text.strOrNull()
                    val weight = inputEditProductWeight.text.doubleOrNull()
                    val category = autoCompleteCategory.text.strOrNull()
                    val weightType = autoCompleteWeight.text.strOrNull()

                    try {
                        val productMap = getProductMap(
                            code = code,
                            name = name,
                            amount = amount,
                            purchasePrice = purchasePrice,
                            salePrice = salePrice,
                            description = description ?: "",
                            category = category ?: "",
                            weight = weight ?: 0.0,
                            weightType = weightType ?: ""
                        )

                        updateProduct(code, productMap)
                    } catch (e: IllegalArgumentException) {
                        requireActivity()
                            .showErrorToast(
                                error = getString(R.string.error_updating_product_text)
                            )
                    }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        with(binding) {

            // Valida Code
            if (inputEditProductCode.text.isNullOrBlank()) {
                tvErrorCode.text = getString(R.string.error_product_code_required)
                tvErrorCode.visibility = View.VISIBLE
                inputEditProductCode.requestFocus()
                isValid = false
            } else if (!inputEditProductCode.text.toString().trim().isAlphanumeric()) {
                tvErrorCode.text = getString(R.string.error_product_code_alphanumeric)
                tvErrorCode.visibility = View.VISIBLE
                inputEditProductCode.requestFocus()
                isValid = false
            } else {
                tvErrorCode.visibility = View.GONE
            }

            // Valida Name
            if (inputEditProductName.text.isNullOrBlank()) {
                tvErrorName.text = getString(R.string.error_product_name_required)
                tvErrorName.visibility = View.VISIBLE
                if (isValid) inputEditProductName.requestFocus()
                isValid = false
            } else {
                tvErrorName.visibility = View.GONE
            }

            // Valida Purchase Price
            if (inputEditProductPurchasePrice.text.doubleOrNull() == null) {
                tvErrorPurchasePrice.text = getString(R.string.error_product_purchase_price_required)
                tvErrorPurchasePrice.visibility = View.VISIBLE
                if (isValid) inputEditProductPurchasePrice.requestFocus()
                isValid = false
            } else {
                tvErrorPurchasePrice.visibility = View.GONE
            }

            // Valida Sale Price
            if (inputEditProductSalePrice.text.doubleOrNull() == null) {
                tvErrorSalePrice.text = getString(R.string.error_product_sale_price_required)
                tvErrorSalePrice.visibility = View.VISIBLE
                if (isValid) inputEditProductSalePrice.requestFocus()
                isValid = false
            } else {
                tvErrorSalePrice.visibility = View.GONE
            }

            // Valida Amount
            if (inputEditProductAmount.text.intOrNull() == null) {
                tvErrorAmount.text = getString(R.string.error_product_amount_required)
                tvErrorAmount.visibility = View.VISIBLE
                if (isValid) inputEditProductAmount.requestFocus()
                isValid = false
            } else {
                tvErrorAmount.visibility = View.GONE
            }
        }
        return isValid
    }

    /*-----------------
    |    Firestore    |
    -----------------*/

    private fun updateProduct(code: String, product: HashMap<String, Comparable<*>>) {
        lifecycleScope.launch {
            if (code.isBlank()) return@launch

            val successInUpdate = viewModel.updateProduct(code, product)

            withContext(Dispatchers.Main) {
                if (successInUpdate) {
                    requireActivity()
                        .showSuccessToast(
                            message = resources.getString(R.string.product_updated)
                        )
                    findNavController().navigateUp()
                } else {
                    requireActivity()
                        .showErrorToast(
                            error = resources.getString(R.string.error_updating_product_text)
                        )
                }
            }
        }
    }

    // Helper functions
    private fun Editable?.strOrNull(): String? {
        val text = this.toString().trim()
        return text.ifEmpty { null }
    }

    private fun Editable?.intOrNull(): Int? {
        return this.toString().trim().toIntOrNull()
    }

    private fun Editable?.doubleOrNull(): Double? {
        return this.toString().replace(",", ".").trim().toDoubleOrNull()
    }

    private fun String.isAlphanumeric(): Boolean {
        return this.matches(Regex("^[a-zA-Z0-9]+$"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
