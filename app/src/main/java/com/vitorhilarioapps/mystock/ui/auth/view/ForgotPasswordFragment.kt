package com.vitorhilarioapps.mystock.ui.auth.view

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentForgotPasswordBinding
import com.vitorhilarioapps.mystock.ui.auth.viewmodel.AuthViewModel
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast

class ForgotPasswordFragment: Fragment() {

    private var _binding : FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentForgotPasswordBinding.inflate(LayoutInflater.from(group?.context), group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        Picasso.get()
            .load("file:///android_asset/sign_up_illustration.png")
            .into(binding.ivForgotPasswordBanner)
    }

    private fun setupClickListeners() {
        binding.btnBackForgotPassword.setOnClickListener {
            val action = ForgotPasswordFragmentDirections.actionForgotPasswordToSignIn()
            findNavController().navigate(action)
        }

        binding.btnSubmitForgotPassword.setOnClickListener {
            binding.inputForgotPassword.text?.strOrNull()?.trim()
                ?.takeIf { it.isNotBlank() && it.isNotEmpty() }
                ?.let { email ->
                    sendPasswordResetEmail(email)
                } ?: run {
                    printError(getString(R.string.wrong_email))
                }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        lifecycleScope.launch {
            val success = viewModel.sendPasswordResetEmail(email)

            withContext(Dispatchers.Main) {
                if (success) {
                    printSuccess(getString(R.string.email_sent))
                } else {
                    printError(getString(R.string.error_sending_email_try_again_text))
                }
            }
        }
    }

    /*---------------
  |    Toast's    |
  ---------------*/
    private fun printSuccess(message: String) {
        requireActivity()
            .showSuccessToast(
                message = message,
                duration = MotionToast.SHORT_DURATION
            )
    }

    private fun printError(error: String) {
        requireActivity()
            .showErrorToast(
                error = error,
                duration = MotionToast.SHORT_DURATION
            )
    }

    private fun Editable?.strOrNull(): String? {
        val text = this.toString().trim()
        return text.ifEmpty { null }
    }
}