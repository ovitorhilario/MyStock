package com.vitorhilarioapps.mystock.ui.auth.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentSignUpBinding
import com.vitorhilarioapps.mystock.ui.auth.viewmodel.AuthViewModel
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast

class SignUpFragment : Fragment() {

    private var _binding : FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    private lateinit var auth: FirebaseAuth
    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSignUpBinding.inflate(LayoutInflater.from(group?.context), group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAuth()
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
            val userName = binding.inputUserNameSignUp.text?.toString()?.trim() ?: ""
            val email = binding.inputEmailSignUp.text?.toString()?.trim() ?: ""
            val password = binding.inputPasswordSignUp.text?.toString()?.trim() ?: ""
            val confirmPassword = binding.inputConfirmPasswordSignUp.text?.toString()?.trim() ?: ""

            val required = listOf(userName, email, password, confirmPassword)
            val anyIsEmptyOrBlank = required.any { it.isBlank() || it.isEmpty() }

            if (!anyIsEmptyOrBlank) {
                if (password == confirmPassword && email.isNotEmpty() && password.isNotEmpty()) {
                    if (password.length >= 6) {
                        signUp(userName, email, password)
                    } else {
                        printError(getString(R.string.little_password))
                    }
                } else {
                    printError(getString(R.string.password_do_not_match))
                }
            } else {
                printError(getString(R.string.fill_in_all_information_text))
            }
        }
    }


    /*---------------------
    |    Firebase Auth    |
    ---------------------*/

    private fun initAuth() {
        auth = Firebase.auth
    }

    private fun signUp(userName: String, email: String, password: String) {
        lifecycleScope.launch {
            val signUpTask = viewModel.createUser(email, password)

            withContext(Dispatchers.Main) {
                if (signUpTask?.isSuccessful == true) {
                    val user = auth.currentUser

                    val updatedUser = userProfileChangeRequest {
                        displayName = userName
                    }

                    user?.updateProfile(updatedUser)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            sendEmailVerification(user)
                        }
                    }

                } else {
                    try {
                        throw signUpTask?.exception!!
                    } catch (_: FirebaseAuthInvalidUserException) {
                        printError(getString(R.string.invalid_user))
                    } catch (_: FirebaseAuthInvalidCredentialsException) {
                        printError(getString(R.string.invalid_credentials))
                    } catch (_: Exception) {
                        printError(getString(R.string.auth_failed))
                    }
                }
            }
        }
    }

    private fun sendEmailVerification(user: FirebaseUser) {
        lifecycleScope.launch {
            val success = viewModel.sendEmailVerification(user)

            withContext(Dispatchers.Main) {
                if (success) {
                    printSuccess(getString(R.string.sended_link))
                } else {
                    printError(getString(R.string.error_sending_email_text))
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

    private fun backToSignIn() {
        val action = SignUpFragmentDirections.actionSignUpToSignIn()
        findNavController().navigate(action)
    }
}