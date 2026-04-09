package com.hadat.aiyoga.login

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class LoginViewModel : BaseViewModel() {

    val loginSuccess = MutableLiveData<Boolean>()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun signInWithFirebase(credential: AuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                loginSuccess.postValue(task.isSuccessful)
            }
    }
}