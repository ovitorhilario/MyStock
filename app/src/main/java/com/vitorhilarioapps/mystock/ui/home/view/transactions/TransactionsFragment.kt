package com.vitorhilarioapps.mystock.ui.home.view.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentTransactionsBinding
import com.vitorhilarioapps.mystock.ui.home.view.transactions.model.TransactionType

class TransactionsFragment : Fragment() {

    private var _binding : FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    // private val args: TransactionsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View? {
        _binding = FragmentTransactionsBinding.inflate(LayoutInflater.from(group?.context), group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        arguments?.let {
            val openRegister = TransactionsFragmentArgs.fromBundle(it).openRegister
            val pageDirection = TransactionsFragmentArgs.fromBundle(it).pageDirection
            arguments?.clear()

            if (openRegister) {
                val transaction = when (pageDirection) {
                    0 -> TransactionType.Sale
                    1 -> TransactionType.Purchase
                    else -> TransactionType.Sale
                }
                val action = TransactionsFragmentDirections.actionTransactionsToSelectProducts(transaction)
                findNavController().navigate(action)
            } else {
                setupTabsAdapter(pageDirection)
            }
        } ?: run {
            setupTabsAdapter(0)
        }
    }

    private fun setupTabsAdapter(page: Int) {

        binding.pagerTransactions.post {
            binding.pagerTransactions.setCurrentItem(page, false)
        }

        val adapter = ActivitiesAdapter(this)
        binding.pagerTransactions.adapter = adapter

        TabLayoutMediator(binding.tabLayoutTransactions, binding.pagerTransactions) { tab, pos ->
            tab.text = when(pos) {
                0 -> resources.getString(R.string.sales)
                1 -> resources.getString(R.string.purchases)
                else -> resources.getString(R.string.sales)
            }
        }.attach()
    }
}

class ActivitiesAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return NestedTransactionFragment.getInstance(position)
    }

    override fun getItemCount() = 2
}