package com.hadat.aiyoga.login

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.firestore.model.User
import com.hadat.aiyoga.firestore.repository.UserRepository
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
class LoginViewModel : BaseViewModel() {

    val loginSuccess = MutableLiveData<Boolean>()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val userRepository = UserRepository()

    fun signInWithFirebase(credential: AuthCredential, context: Context) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        handleUserStorage(firebaseUser, context)
                    } else {
                        loginSuccess.postValue(false)
                    }
                } else {
                    loginSuccess.postValue(false)
                }
            }
    }

    private fun handleUserStorage(firebaseUser: FirebaseUser, context: Context) {
        viewModelScope.launch {
            val uid = firebaseUser.uid
            val exists = userRepository.isUserExists(uid)

            if (!exists) {
                val newUser = User(
                    uid = uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl.toString()
                )
                val isSaved = userRepository.saveUser(newUser)
                if (isSaved) {
                    AppPreferences.setLoggedIn(context, true)
                    loginSuccess.postValue(true)
                } else {
                    loginSuccess.postValue(false)
                }
            } else {
                AppPreferences.setLoggedIn(context, true)
                loginSuccess.postValue(true)
            }
        }
    }
}