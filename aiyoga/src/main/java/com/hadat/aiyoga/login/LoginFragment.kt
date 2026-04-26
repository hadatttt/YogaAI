package com.hadat.aiyoga.login

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentLoginBinding
import com.hadat.aiyoga.utils.service.AppPreferences
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
                    showToast(getString(R.string.google_account_is_null))
                    return@registerForActivityResult
                }

                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                viewModel.signInWithFirebase(credential, requireContext())

            } catch (e: ApiException) {
                showToast(getString(R.string.google_sign_in_failed, e.statusCode))
            }
        }

    override fun initView() {
        setupGoogleSignIn()
        updateLanguageUI(AppPreferences.getLanguageCode(requireContext()), isAnim = false)
    }

    override fun initListener() {
        binding.btnGoogle.singleClick {
            launchGoogleSignIn()
        }
        binding.btnLanguageToggle.singleClick {
            val currentLang = AppPreferences.getLanguageCode(requireContext())
            val newLang = if (currentLang == "en") "vi" else "en"

            AppPreferences.setLanguageCode(requireContext(), newLang)
            updateLanguageUI(newLang, isAnim = true)

            binding.root.postDelayed({
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(newLang)
                )
                requireActivity().recreate()
            }, 250)
        }
    }

    override fun initData() {

        viewModel.loginSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                val isComplete = viewModel.isProfileComplete.value ?: false
                if (isComplete) {
                    navigate(R.id.homeFragment, isPop = true)
                } else {
                    navigate(R.id.informationFragment, isPop = true)
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            showToast(message)
        }
    }
    private fun updateLanguageUI(langCode: String, isAnim: Boolean) {
        if (!isAdded) return

        binding.viewSelection.post {
            if (!isAdded) return@post

            binding.apply {
                val isEn = langCode == "en"
                val targetX = if (isEn) 0f else viewSelection.width.toFloat()

                if (isAnim) {
                    viewSelection.animate()
                        .translationX(targetX)
                        .setDuration(250)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                } else {
                    viewSelection.animate().cancel()
                    viewSelection.translationX = targetX
                }
                tvEn.setTextColor(if (isEn) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                tvVi.setTextColor(if (isEn) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
            }
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