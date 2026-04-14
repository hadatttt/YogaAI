package com.hadat.aiyoga.login

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.hadat.aiyoga.data.firestore.model.User
import com.hadat.aiyoga.data.firestore.repository.UserRepository
import com.hadat.aiyoga.service.AppPreferences
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel() {

    val loginSuccess = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val userRepository by lazy { UserRepository() }
    fun signInWithFirebase(
        credential: AuthCredential,
        context: Context
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    handleError("Authentication failed")
                    return@addOnCompleteListener
                }

                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    handleError("User is null")
                    return@addOnCompleteListener
                }

                handleUser(firebaseUser, context)
            }
    }
    private fun handleUser(
        firebaseUser: FirebaseUser,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val uid = firebaseUser.uid

                val exists = userRepository.isUserExists(uid)

                if (!exists) {
                    val newUser = mapFirebaseUserToModel(firebaseUser)
                    val isSaved = userRepository.saveUser(newUser)

                    if (!isSaved) {
                        handleError("Save user failed")
                        return@launch
                    }
                }

                saveLoginSession(context, uid)
                loginSuccess.postValue(true)

            } catch (e: Exception) {
                handleError(e.message ?: "Unknown error")
            }
        }
    }

    private fun mapFirebaseUserToModel(firebaseUser: FirebaseUser): User {
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email.orEmpty(),
            displayName = firebaseUser.displayName.orEmpty(),
            photoUrl = firebaseUser.photoUrl?.toString().orEmpty()
        )
    }

    private fun saveLoginSession(
        context: Context,
        uid: String
    ) {
        AppPreferences.setLoginStatus(context, true, uid)
    }

    private fun handleError(message: String) {
        errorMessage.postValue(message)
        loginSuccess.postValue(false)
    }
}