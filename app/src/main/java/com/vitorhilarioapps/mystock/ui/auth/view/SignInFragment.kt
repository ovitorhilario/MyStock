package com.vitorhilarioapps.mystock.ui.auth.view

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentSignInBinding
import com.vitorhilarioapps.mystock.ui.auth.viewmodel.AuthViewModel
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast

class SignInFragment : Fragment() {

    private var _binding : FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    // Firebase
    private lateinit var auth: FirebaseAuth

    // Google SignIn
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var signUpRequest: BeginSignInRequest

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

        try {
            val user = auth.currentUser
            if (user != null && user.isEmailVerified) {
                updateUI()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in capture saved user.")
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
        try {
            auth = Firebase.auth
            oneTapClient = Identity.getSignInClient(requireActivity())

            signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.web_client_id))
                        .setFilterByAuthorizedAccounts(true)
                        .build())
                .build()

            signUpRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.web_client_id))
                        // Show all accounts on the device.
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build()
        } catch (e: Exception) {
            printError(getString(R.string.error_initializing_authentication_text))
        }
    }

    private fun signInGoogle() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(requireActivity()) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(requireActivity()) { e ->
                // No Sign-Up Google Accounts found.
                signUpGoogle()
                Log.d(TAG, e.localizedMessage ?: "")
            }
    }

    private fun signUpGoogle() {
        oneTapClient.beginSignIn(signUpRequest)
            .addOnSuccessListener(requireActivity()) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(requireActivity()) { e ->
                // No Google Accounts found.
                requireActivity()
                    .showErrorToast(
                        error = getString(R.string.no_google_accounts_found_text)
                    )

                Log.d(TAG, e.localizedMessage)
            }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val googleCredential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = googleCredential.googleIdToken
                    when {
                        idToken != null -> {
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener(requireActivity()) { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        if (user != null) {
                                            updateUI()
                                        }
                                    } else {
                                        printError(getString(R.string.sign_in_fail_text))
                                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                                    }
                                }
                        }
                        else -> {
                            printError(getString(R.string.sign_in_fail_text))
                            Log.d(TAG, "No ID token!")
                        }
                    }
                } catch (e: ApiException) {

                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
                            // Don't re-prompt the user.
                            showOneTapUI = false
                        }
                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(TAG, "One-tap encountered a network error.")
                            // Try again or just ignore.
                            printError(getString(R.string.network_error_text))
                        }
                        else -> {
                            printError(getString(R.string.error_getting_credential_text))
                            Log.d(TAG, "Couldn't get credential from result." + " (${e.localizedMessage})")
                        }
                    }
                }
            }
        }
    }

    /*---------------------
    |    Firebase Auth    |
    ---------------------*/

    private fun signIn(email: String, password: String) {
        lifecycleScope.launch {
            val signInTask = viewModel.signIn(email, password)

            withContext(Dispatchers.Main) {
                if (signInTask?.isSuccessful == true) {
                    val user = auth.currentUser

                    user?.let {
                        if (it.isEmailVerified) {
                            updateUI()
                        } else {
                            showDialogVerifyEmail(it)
                        }
                    } ?: run {
                        printError(getString(R.string.error_sign_in))
                    }
                } else {
                    printError(getString(R.string.error_sign_in))
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

    private fun showDialogVerifyEmail(user: FirebaseUser) {
        MaterialAlertDialogBuilder(requireActivity(),
            com.google.android.material.R.style.MaterialAlertDialog_Material3)
            .setMessage(R.string.send_new_link)
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                sendEmailVerification(user)
            }
            .setPositiveButton(resources.getString(R.string.send)) { _, _ -> }
            .show()
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

    companion object {
        const val TAG = "SignInFragment"
        private const val REQ_ONE_TAP = 2  // Can be any integer unique to the Activity
        private var showOneTapUI = true
    }
}