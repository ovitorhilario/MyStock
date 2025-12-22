package com.vitorhilarioapps.mystock.ui.auth.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.databinding.FragmentSignInBinding
import com.vitorhilarioapps.mystock.ui.auth.viewmodel.AuthViewModel
import com.vitorhilarioapps.mystock.utils.generateNonce
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignInFragment : Fragment() {

    private var _binding : FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private lateinit var signInRequestAuto: GetCredentialRequest
    private lateinit var signInRequestButton: GetCredentialRequest

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSignInBinding.inflate(LayoutInflater.from(group?.context), group, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        credentialManager = CredentialManager.create(requireContext())
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser

        if (currentUser != null && currentUser.isEmailVerified) {
            updateUI()
        } else {
            launchCredentialManager()
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

    private fun launchCredentialManager() {
        val googleIdOptionAuto = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(getString(R.string.web_client_id))
            .setAutoSelectEnabled(true)
            .setNonce(generateNonce())
            .build()

        signInRequestAuto = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOptionAuto)
            .build()

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = getString(R.string.web_client_id)
        ).setNonce(generateNonce())
            .build()

        signInRequestButton = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val disableAutoSignIn = requireActivity().intent.getBooleanExtra("DISABLE_AUTO_SIGN_IN", false)
        if (!disableAutoSignIn) {
            checkAutoSignIn()
        }
    }

    fun handleSignInWithGoogleOption(result: GetCredentialResponse) {
        val credential = result.credential

        when (val customCredential = credential) {
            is CustomCredential -> {
                if (customCredential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(customCredential.data)

                        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    Log.e(TAG, "Unexpected type of credential")
                }
            }

            else -> {
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    private fun checkAutoSignIn() {
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = requireContext(),
                    request = signInRequestAuto
                )
                handleSignInWithGoogleOption(result)
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "GetCredentialCancellationException", e)
            } catch (e: NoCredentialException) {
                Log.e(TAG, "NoCredentialException", e)
                printError(getString(R.string.error_auth_no_credential))
            } catch (e: GetCredentialInterruptedException) {
                Log.e(TAG, "GetCredentialInterruptedException", e)
                printError(getString(R.string.error_auth_get_credential_interrupted))
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Erro no login manual: ${e.message}", e)
                printError(getString(R.string.error_auth_get_credential))
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    if (user != null) {
                        updateUI()
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun signInGoogle() {
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = requireContext(),
                    request = signInRequestButton
                )
                handleSignInWithGoogleOption(result)

            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "GetCredentialCancellationException", e)
            } catch (e: NoCredentialException) {
                Log.e(TAG, "NoCredentialException", e)
                printError(getString(R.string.error_auth_no_credential))
            } catch (e: GetCredentialInterruptedException) {
                Log.e(TAG, "GetCredentialInterruptedException", e)
                printError(getString(R.string.error_auth_get_credential_interrupted))
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Erro no login manual: ${e.message}", e)
                printError(getString(R.string.error_auth_get_credential))
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
            )
    }

    private fun printError(error: String) {
        requireActivity()
            .showErrorToast(
                error = error,
            )
    }

    companion object {
        const val TAG = "SignInFragment"
    }
}
