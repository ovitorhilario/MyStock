package com.vitorhilarioapps.mystock.ui.home.view.addproducts

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
            if (validateInputs()) {
                with(binding) {

                    val code = inputAddProductCode.text.strOrNull() ?: ""
                    val name = inputAddProductName.text.strOrNull() ?: ""
                    val purchasePrice = inputAddProductPurchasePrice.text.doubleOrNull() ?: 0.0
                    val salePrice = inputAddProductSalePrice.text.doubleOrNull() ?: 0.0
                    val amount = inputAddProductAmount.text.intOrNull() ?: 0

                    // Additional - can be null
                    val description = inputAddProductDescription.text.strOrNull()
                    val weight = inputAddProductWeight.text.doubleOrNull()
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

                        addProduct(code, productMap)
                    } catch (e: IllegalArgumentException) {
                        requireActivity()
                            .showErrorToast(
                                error = getString(R.string.error_registering_product_text)
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
            if (inputAddProductCode.text.isNullOrBlank()) {
                tvErrorCode.text = getString(R.string.error_product_code_required)
                tvErrorCode.visibility = View.VISIBLE
                inputAddProductCode.requestFocus()
                isValid = false
            } else if (!inputAddProductCode.text.toString().trim().isAlphanumeric()) {
                tvErrorCode.text = getString(R.string.error_product_code_alphanumeric)
                tvErrorCode.visibility = View.VISIBLE
                inputAddProductCode.requestFocus()
                isValid = false
            } else {
                tvErrorCode.visibility = View.GONE
            }

            // Valida Name
            if (inputAddProductName.text.isNullOrBlank()) {
                tvErrorName.text = getString(R.string.error_product_name_required)
                tvErrorName.visibility = View.VISIBLE
                if (isValid) inputAddProductName.requestFocus() // Foca apenas se for o primeiro erro
                isValid = false
            } else {
                tvErrorName.visibility = View.GONE
            }

            // Valida Purchase Price (Verifica conversão numérica)
            if (inputAddProductPurchasePrice.text.doubleOrNull() == null) {
                tvErrorPurchasePrice.text = getString(R.string.error_product_purchase_price_required)
                tvErrorPurchasePrice.visibility = View.VISIBLE
                if (isValid) inputAddProductPurchasePrice.requestFocus()
                isValid = false
            } else {
                tvErrorPurchasePrice.visibility = View.GONE
            }

            // Valida Sale Price (Verifica conversão numérica)
            if (inputAddProductSalePrice.text.doubleOrNull() == null) {
                tvErrorSalePrice.text = getString(R.string.error_product_sale_price_required)
                tvErrorSalePrice.visibility = View.VISIBLE
                if (isValid) inputAddProductSalePrice.requestFocus()
                isValid = false
            } else {
                tvErrorSalePrice.visibility = View.GONE
            }

            // Valida Amount (Verifica conversão para Inteiro)
            if (inputAddProductAmount.text.intOrNull() == null) {
                tvErrorAmount.text = getString(R.string.error_product_amount_required)
                tvErrorAmount.visibility = View.VISIBLE
                if (isValid) inputAddProductAmount.requestFocus()
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

    private fun addProduct(code: String, product: HashMap<String, Comparable<*>>) {
        lifecycleScope.launch {
            // Evita processar se o código for vazio (dupla checagem)
            if (code.isBlank()) return@launch

            val hasProduct = viewModel.hasProduct(code)

            withContext(Dispatchers.Main) {
                if (!hasProduct) {
                    val successInAdd = viewModel.addProduct(code, product)

                    if (successInAdd) {
                        requireActivity()
                            .showSuccessToast(
                                message = resources.getString(R.string.product_added)
                            )
                        // Só navega se deu certo
                        findNavController().navigateUp()
                    } else {
                        // Caso a operação no Firestore falhe
                        requireActivity()
                            .showErrorToast(
                                error = resources.getString(R.string.error_registering_product_text)
                            )
                    }
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
                binding.inputAddProductCode.setText(rawValue)
            }
            .addOnFailureListener { e ->
                requireActivity()
                    .showErrorToast(
                        error = buildString { append(resources.getString(R.string.error), e.message.toString()) }
                    )
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