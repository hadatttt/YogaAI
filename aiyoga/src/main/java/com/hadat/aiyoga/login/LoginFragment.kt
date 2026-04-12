package com.hadat.aiyoga.login

import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentLoginBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick
import androidx.activity.result.contract.ActivityResultContracts

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginViewModel>() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            viewModel.signInWithFirebase(credential, requireContext())
        } catch (e: ApiException) {
        }
    }

    override fun initView() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun initListener() {
        binding.btnGoogle.singleClick {
            val signInIntent = googleSignInClient.signInIntent
            googleLauncher.launch(signInIntent)
        }
    }

    override fun initData() {
        viewModel.loginSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.loginSuccess.value = false
                navigate(R.id.homeFragment)
            } else {
            }
        }
    }
}