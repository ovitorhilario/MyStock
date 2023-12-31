package com.vitorhilarioapps.mystock.ui.auth.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentValidateFirestoreBinding
import com.vitorhilarioapps.mystock.ui.auth.viewmodel.AuthViewModel
import com.vitorhilarioapps.mystock.ui.home.HomeActivity
import com.vitorhilarioapps.mystock.utils.showErrorToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast

class ValidateFireStoreFragment : Fragment() {

    private var _binding : FragmentValidateFirestoreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    /*-----------------
    |    LifeCycle    |
    -----------------*/

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentValidateFirestoreBinding.inflate(LayoutInflater.from(group?.context), group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        validateFireStore()
    }

    private fun setupUI() {
        Glide.with(this)
            .load(ContextCompat.getDrawable(requireContext(), R.drawable.pac_man_loading))
            .into(binding.ivValidateLoading)
    }

    private fun startHomePage() {
        requireActivity().startActivity(Intent(this.context, HomeActivity::class.java))
        requireActivity().finish()
    }

    private fun backToSignIn() {
        val action = ValidateFireStoreFragmentDirections.actionValidateFirestoreToSignIn()
        findNavController().navigate(action)
    }

    /*----------------
    |    Firebase    |
    ----------------*/

    private fun validateFireStore() {
        try {
            val auth = Firebase.auth
            val user = auth.currentUser

            if (user != null && user.isEmailVerified) {
                checkUserDb(user)
            } else {
                printError(getString(R.string.user_don_t_found_text))
                backToSignIn()
            }
        } catch (e: Exception) {
            printError(getString(R.string.error_in_validation_text))
            backToSignIn()
        }
    }

    private fun checkUserDb(user: FirebaseUser) {
        lifecycleScope.launch {
            val hasUserInDb = viewModel.hasUserInDb(user.uid)

            withContext(Dispatchers.Main) {
                if (hasUserInDb) {
                    startHomePage()
                } else {
                    createDbForUser(user)
                }
            }
        }
    }

    private fun createDbForUser(user: FirebaseUser) {
        lifecycleScope.launch {
            val success = viewModel.createDbForUser(user)

            withContext(Dispatchers.Main) {
                if (success) {
                    startHomePage()
                } else {
                    backToSignIn()
                }
            }
        }
    }

    /*---------------
    |    Toast's    |
    ---------------*/
    private fun printError(error: String) {
        requireActivity()
            .showErrorToast(
                error = error,
                duration = MotionToast.SHORT_DURATION
            )
    }
}