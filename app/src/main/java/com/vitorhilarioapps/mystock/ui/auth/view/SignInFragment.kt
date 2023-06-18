package com.vitorhilarioapps.mystock.ui.auth.view

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentSignInBinding
import com.vitorhilarioapps.mystock.ui.auth.viewmodel.AuthViewModel
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showInfoToast
import kotlinx.coroutines.launch
import www.sanju.motiontoast.MotionToast

class SignInFragment : Fragment() {

    private var _binding : FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSignInBinding.inflate(LayoutInflater.from(group?.context), group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSignInClient()
        setupUI()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()

        if (viewModel.isAuthenticated()) {
            updateUI()
        }
    }

    /*-----------------
    |    Handle UI    |
    -----------------*/

    private fun setupUI() {
        Picasso.get()
            .load("file:///android_asset/sign_in_illustration.png")
            .into(binding.ivSignInBanner)
    }

    private fun setupClickListeners() {

        binding.anchorSignUp.setOnClickListener {
            val action = SignInFragmentDirections.actionSignInToSignUp()
            findNavController().navigate(action)
        }

        binding.anchorForgotPassword.setOnClickListener {
            val action = SignInFragmentDirections.actionSignInToForgotPassword()
            findNavController().navigate(action)
        }

        binding.btnSignIn.setOnClickListener {
            getInputs()?.let { result ->
                val (email, password) = result
                signIn(email, password)
            }
        }

        binding.btnSignInGoogle.setOnClickListener { signInGoogle() }
    }

    private fun updateUI() {
        val action = SignInFragmentDirections.actionSignInToValidateFirestore()
        findNavController().navigate(action)
    }

    private fun getInputs() : Pair<String, String>? {
        val email = binding.inputEmailSignIn.text?.trim().toString()
        val password = binding.inputPasswordSignIn.text?.trim().toString()

        return if (email.isNotEmpty() && password.isNotEmpty()) {
            email to password
        } else null
    }

    /*-------------------
    |    Google Auth    |
    -------------------*/

    private fun initSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun signInWithCredential(idToken: String) {
        lifecycleScope.launch {
            val user = viewModel.signInWithCredential(idToken)

            user?.let {
                updateUI()
            } ?: run {
                requireActivity()
                    .showErrorToast(
                        error = resources.getString(R.string.error_sign_in_google),
                        duration = MotionToast.SHORT_DURATION)
            }
        }
    }

    private val signInGoogleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val resultCode = activityResult.resultCode
        val data = activityResult.data

        if (resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                signInWithCredential(account.idToken!!)
            } catch (e: ApiException) {
                requireActivity()
                    .showErrorToast(
                        error = resources.getString(R.string.error_sign_in_google),
                        duration = MotionToast.SHORT_DURATION)
            }
        }
    }

    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInGoogleLauncher.launch(signInIntent)
    }

    /*---------------------
    |    Firebase Auth    |
    ---------------------*/

    private fun signIn(email: String, password: String) {
        lifecycleScope.launch {
            val signInTask = viewModel.signIn(email, password)
            val user = signInTask?.result?.user

            if (signInTask?.isSuccessful == true && user != null) {
                if (user.isEmailVerified) {
                    updateUI()
                } else {
                    showDialogVerifyEmail()
                }
            } else {
                requireActivity()
                    .showErrorToast(
                        error = resources.getString(R.string.error_sign_in),
                        duration = MotionToast.SHORT_DURATION)
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
                    .showErrorToast(error = getString(R.string.error_sending_email_text))
            }
        }
    }

    private fun showDialogVerifyEmail() {
        MaterialAlertDialogBuilder(requireActivity(),
            com.google.android.material.R.style.MaterialAlertDialog_Material3)
            .setMessage(R.string.send_new_link)
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                sendEmailVerification()
            }
            .setPositiveButton(resources.getString(R.string.send)) { _, _ -> }
            .show()
    }
}