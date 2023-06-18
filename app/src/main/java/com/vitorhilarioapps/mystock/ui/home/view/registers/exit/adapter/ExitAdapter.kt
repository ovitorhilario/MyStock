package com.vitorhilarioapps.mystock.ui.home.view.registers.exit.adapter

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.ProductSelectItemBinding
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.utils.moneyType

class ExitAdapter (
    private val data: List<Product>,
    private val addProductCont: (Int) -> Int,
    private val removeProductCont: (Int) -> Int,
    private val resources: Resources
) : Adapter<ExitAdapter.SingleHolder>() {

    inner class SingleHolder(binding: ProductSelectItemBinding) : ViewHolder(binding.root) {
        private val tvProductSelectName = binding.tvProductSelectName
        private val tvProductSelectAmount = binding.tvProductSelectAmount
        private val tvProductSelectCont = binding.tvProductSelectCont
        private val tvProductSelectPrice = binding.tvProductSelectPrice
        private val ivProductSelectPriceIcon = binding.ivProductSelectPriceIcon
        private val btnProductSelectAdd = binding.btnProductSelectAdd
        private val btnProductSelectMinus = binding.btnProductSelectMinus

        private fun updateCont(cont: Int) {
            tvProductSelectCont.text = cont.toString()
        }

        fun bind(product: Product) {
            tvProductSelectName.text = product.name
            tvProductSelectAmount.text = product.amount.toString()
            tvProductSelectCont.text = "0"
            tvProductSelectPrice.text = product.purchasePrice.moneyType()
            ivProductSelectPriceIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_purchase))

            btnProductSelectAdd.setOnClickListener {
                val cont = addProductCont(product.code)
                updateCont(cont)
            }

            btnProductSelectMinus.setOnClickListener {
                val cont = removeProductCont(product.code)
                updateCont(cont)
            }
        }
    }

    override fun onCreateViewHolder(group: ViewGroup, viewType: Int) : SingleHolder {
        return SingleHolder(
            ProductSelectItemBinding.inflate(LayoutInflater.from(group.context), group, false)
        )
    }

    override fun onBindViewHolder(holder: ExitAdapter.SingleHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size
}