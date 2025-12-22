package com.vitorhilarioapps.mystock.ui.home.view.profile

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.credentials.ClearCredentialStateRequest
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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.squareup.picasso.Picasso
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.data.model.UserDb
import com.vitorhilarioapps.mystock.databinding.FragmentProfileBinding
import com.vitorhilarioapps.mystock.ui.auth.AuthActivity
import com.vitorhilarioapps.mystock.ui.auth.view.SignInFragment.Companion.TAG
import com.vitorhilarioapps.mystock.ui.home.viewmodel.FirestoreViewModel
import com.vitorhilarioapps.mystock.utils.generateNonce
import com.vitorhilarioapps.mystock.utils.showErrorToast
import com.vitorhilarioapps.mystock.utils.showSuccessToast
import com.vitorhilarioapps.mystock.utils.toFormattedDate
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding : FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel : FirestoreViewModel by activityViewModels()

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        credentialManager = CredentialManager.create(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, saved: Bundle?): View {
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

        viewModel.deleteState.observe(viewLifecycleOwner) { state ->
            when(state) {
                is FirestoreViewModel.DeleteAccountState.Loading -> {
                    binding.btnProfileDeleteAccount.isEnabled = false
                }
                is FirestoreViewModel.DeleteAccountState.Success -> {
                    Toast.makeText(requireContext(),
                        getString(R.string.success_delete_account), Toast.LENGTH_LONG).show()
                    viewModel.resetDeleteState()

                    logoutAndGoToAuth()
                }
                is FirestoreViewModel.DeleteAccountState.Failure -> {
                    binding.btnProfileDeleteAccount.isEnabled = true
                    Toast.makeText(requireContext(),
                        getString(R.string.error_delete_account), Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteState()
                }
                is FirestoreViewModel.DeleteAccountState.ReLoginRequired -> {
                    checkProviderAndReAuth()
                    viewModel.resetDeleteState()
                }
                is FirestoreViewModel.DeleteAccountState.Idle -> {
                    binding.btnProfileDeleteAccount.isEnabled = true
                }
            }
        }
    }

    private fun checkProviderAndReAuth() {
        if (viewModel.isGoogleAccount()) {
            launchGoogleReAuth()
        } else {
            showReAuthDialog()
        }
    }

    private fun launchGoogleReAuth() {
        lifecycleScope.launch {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                serverClientId = getString(R.string.web_client_id)
            ).setNonce(generateNonce())
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            try {
                val result = credentialManager.getCredential(requireContext(), request)
                handleReAuthWithGoogle(result)
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

    private fun handleReAuthWithGoogle(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                viewModel.reauthenticateAndDelete(googleCredential)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("ProfileFragment", "Received an invalid google id token response", e)
                Toast.makeText(requireContext(), "Falha ao verificar o token do Google.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("ProfileFragment", "Unexpected type of credential")
            Toast.makeText(requireContext(), "Tipo de credencial inesperado.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupUI(user: UserDb) {
        binding.tvProfileUserRegistration.text = user.since.toFormattedDate()
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
            val intent = Intent(requireActivity(), AuthActivity::class.java).apply {
                putExtra("DISABLE_AUTO_SIGN_IN", true)
            }
            startActivity(intent)
            requireActivity().finish()
        }
        binding.btnProfileDeleteAccount.setOnClickListener {
            dialogDeleteConfirmation()
        }
    }

    private fun logoutAndGoToAuth() {
        viewModel.getAuth().signOut()

        lifecycleScope.launch {
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
        }

        val intent = Intent(requireActivity(), AuthActivity::class.java).apply {
            putExtra("DISABLE_AUTO_SIGN_IN", true)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun dialogDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_delete_account_title))
            .setMessage(getString(R.string.dialog_delete_account_description))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(getString(R.string.btn_confirm)) { _, _ ->
                viewModel.deleteAccount()
            }
            .show()
    }

    private fun showReAuthDialog() {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = getString(R.string.hint_current_password)

        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50
        params.rightMargin = 50
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_confirm_account_title))
            .setMessage(getString(R.string.dialog_confirm_account_description))
            .setView(container)
            .setNeutralButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) {
                    viewModel.reauthenticateAndDelete(password)
                } else {
                    Toast.makeText(context,
                        getString(R.string.error_password_is_required), Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

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
}