package com.hadat.aiyoga.login

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class LoginViewModel : BaseViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val loginSuccess = MutableLiveData<FirebaseUser?>()
    val loginError = MutableLiveData<String>()

    fun signInWithCredential(credential: AuthCredential) {
        isLoading.value = true
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    loginSuccess.value = auth.currentUser
                } else {
                    loginError.value = task.exception?.message ?: "Login Failed"
                }
            }
    }
}