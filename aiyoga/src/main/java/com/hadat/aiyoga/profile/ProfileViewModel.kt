package com.hadat.aiyoga.profile

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.firestore.model.User
import com.hadat.aiyoga.data.firestore.repository.UserRepository
import com.hadat.aiyoga.service.AppPreferences
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class ProfileViewModel : BaseViewModel() {
    private val userRepository = UserRepository()

    val userData = MutableLiveData<User?>()
    val saveStatus = MutableLiveData<Boolean?>(null)

    fun loadUser(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            userData.postValue(userRepository.getUser(userId))
        }
    }

    fun updateProfile(userId: String, displayName: String, photoUrl: String) {
        if (userId.isBlank()) {
            saveStatus.value = false
            return
        }
        viewModelScope.launch {
            val ok = userRepository.updateProfile(userId, displayName, photoUrl)
            saveStatus.postValue(ok)
            if (ok) loadUser(userId)
        }
    }

    fun resetSaveStatus() {
        saveStatus.value = null
    }

    fun logout(context: Context, onLogoutSuccess: () -> Unit) {
        FirebaseAuth.getInstance().signOut()

        AppPreferences.logout(context)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(context, gso)
        googleClient.signOut().addOnCompleteListener {
            onLogoutSuccess()
        }.addOnFailureListener {
            onLogoutSuccess()
        }
    }

}

