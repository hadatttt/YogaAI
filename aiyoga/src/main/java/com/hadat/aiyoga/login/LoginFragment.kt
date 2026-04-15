package com.hadat.aiyoga.login

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentLoginBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginViewModel>() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account = task.getResult(ApiException::class.java)

                if (account == null) {
                    showToast("Google account is null")
                    return@registerForActivityResult
                }

                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                viewModel.signInWithFirebase(credential, requireContext())

            } catch (e: ApiException) {
                showToast("Google sign-in failed: ${e.statusCode}")
            }
        }

    override fun initView() {
        setupGoogleSignIn()
    }

    override fun initListener() {
        binding.btnGoogle.singleClick {
            launchGoogleSignIn()
        }
    }

    override fun initData() {

        viewModel.loginSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                navigate(R.id.homeFragment)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            showToast(message)
        }
    }
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun launchGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleLauncher.launch(signInIntent)
    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}