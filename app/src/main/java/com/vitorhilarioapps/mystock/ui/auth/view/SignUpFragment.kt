package com.vitorhilarioapps.mystock.ui.auth.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentSignUpBinding
import com.vitorhilarioapps.mystock.ui.auth.viewmodel.AuthViewModel
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showInfoToast
import kotlinx.coroutines.launch
import www.sanju.motiontoast.MotionToast

class SignUpFragment : Fragment() {

    private var _binding : FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSignUpBinding.inflate(LayoutInflater.from(group?.context), group, false)
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
        Picasso.get()
            .load("file:///android_asset/sign_up_illustration.png")
            .into(binding.ivSignUpBanner)
    }

    private fun setupClickListeners() {

        binding.btnBackSignUp.setOnClickListener {
            val action = SignUpFragmentDirections.actionSignUpToSignIn()
            findNavController().navigate(action)
        }

        binding.btnSignUp.setOnClickListener {
            val userName = binding.inputUserNameSignUp.text?.trim().toString()
            val email = binding.inputEmailSignUp.text?.trim().toString()
            val password = binding.inputPasswordSignUp.text?.trim().toString()
            val confirmPassword = binding.inputConfirmPasswordSignUp.text?.trim().toString()

            if (userName.isNotEmpty() && userName.isNotBlank()) {
                if (password == confirmPassword && email.isNotEmpty() && password.isNotEmpty()) {
                    if (password.length >= 6) {
                        signUp(userName, email, password)
                    } else {
                        requireActivity()
                            .showErrorToast(
                                error = resources.getString(R.string.little_password),
                                duration = MotionToast.SHORT_DURATION
                            )
                    }
                } else {
                    requireActivity()
                        .showErrorToast(
                            error = resources.getString(R.string.password_do_not_match),
                            duration = MotionToast.SHORT_DURATION
                        )
                }
            } else {
                requireActivity()
                    .showErrorToast(
                        error = getString(R.string.fill_in_all_information_text),
                        duration = MotionToast.SHORT_DURATION
                    )
            }
        }
    }

    /*---------------------
    |    Firebase Auth    |
    ---------------------*/

    private fun signUp(userName: String, email: String, password: String) {
        lifecycleScope.launch {
            val signUpTask = viewModel.createUser(email, password)
            val user = signUpTask?.result?.user

            if (signUpTask?.isSuccessful == true && user != null) {
                val updatedUser = userProfileChangeRequest {
                    displayName = userName
                }

                user.updateProfile(updatedUser)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            sendEmailVerification()
                            backToSignIn()
                        }
                    }
            } else {
                try {
                    throw signUpTask?.exception!!
                } catch (_: FirebaseAuthInvalidUserException) {
                    invalidUser()
                } catch (_: FirebaseAuthInvalidCredentialsException) {
                    invalidCredentials()
                } catch (_: Exception) {
                    errorOnCreate()
                }
            }
        }
    }

    private fun sendEmailVerification() {
        lifecycleScope.launch {
            val success = viewModel.sendEmailVerification()

            if (success) {
                requireActivity()
                    .showInfoToast(info = resources.getString(R.string.sended_link))
            } else {
                requireActivity()
                    .showErrorToast(
                        error = getString(R.string.error_sending_email_text)
                    )
            }
        }
    }

    private fun invalidUser() {
        requireActivity()
            .showErrorToast(
                error = resources.getString(R.string.invalid_user),
                duration = MotionToast.SHORT_DURATION
            )
    }

    private fun invalidCredentials() {
        requireActivity()
            .showErrorToast(
                error = resources.getString(R.string.invalid_credentials),
                duration = MotionToast.SHORT_DURATION
            )
    }

    private fun errorOnCreate() {
        requireActivity()
            .showErrorToast(
                error = resources.getString(R.string.auth_failed),
                duration = MotionToast.SHORT_DURATION
            )
    }

    private fun backToSignIn() {
        val action = SignUpFragmentDirections.actionSignUpToSignIn()
        findNavController().navigate(action)
    }
}