package com.vitorhilarioapps.mystock.ui.home.view.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.data.model.UserDb
import com.vitorhilarioapps.mystock.databinding.FragmentProfileBinding
import com.vitorhilarioapps.mystock.ui.auth.AuthActivity
import com.vitorhilarioapps.mystock.ui.home.viewmodel.FirestoreViewModel
import java.text.SimpleDateFormat
import java.util.Date

class ProfileFragment : Fragment() {
    private var _binding : FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel : FirestoreViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View? {
        _binding = FragmentProfileBinding.inflate(inflater, group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getDataForProfile()
        setupObservers()
    }

    private fun getDataForProfile() {
        viewModel.getUserFromDb()
    }

    private fun setupObservers() {
        viewModel.userDb.observe(viewLifecycleOwner) { userDb ->
            userDb?.let {
                setupUI(userDb)
                setupClickListeners()
            }
        }
    }

    private fun setupUI(user: UserDb) {
        binding.tvProfileUserRegistration.text = user.since.toDate().getDateInBars()
        binding.tvProfileUserName.text = user.name
        binding.tvProfileUserEmail.text = user.email

        Picasso.get()
            .load("file:///android_asset/banner.png")
            .into(binding.ivProfileBannerUser)

        user.photoUrl?.let { uri ->
            Picasso.get()
                .load(uri)
                .error(R.drawable.user_avatar)
                .into(binding.ivProfileUser)
        }
    }

    private fun setupClickListeners() {
        binding.btnProfileSignOut.setOnClickListener {
            viewModel.getAuth().signOut()
            startActivity(Intent(requireActivity(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    fun Date.getDateInBars() : String {
        val dt = SimpleDateFormat("dd/MM/yyyy")
        return dt.format(this)
    }
}