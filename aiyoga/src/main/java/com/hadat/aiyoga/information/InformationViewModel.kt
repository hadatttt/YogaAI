package com.hadat.aiyoga.information

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hadat.aiyoga.data.firestore.model.HealthProfileModel
import com.hadat.aiyoga.data.firestore.repository.HealthProfileRepository
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.utils.yogautils.HealthCalculatorUtils
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class InformationViewModel : BaseViewModel() {

    private val repository = HealthProfileRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _saveStatus = MutableLiveData<Boolean?>()
    val saveStatus: LiveData<Boolean?> = _saveStatus

    private val _profileData = MutableLiveData<HealthProfileModel?>()
    val profileData: LiveData<HealthProfileModel?> = _profileData

    private val _isLoading = MutableLiveData<Boolean>()

    fun fetchProfile() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val profile = repository.getProfile(userId)
            _profileData.value = profile
            _isLoading.value = false
        }
    }

    fun saveHealthProfile(
        context: Context,
        gender: String,
        age: Int,
        weight: Float,
        height: Float,
        activityLevelPos: Int
    ) {
        val userId = auth.currentUser?.uid ?: return

        val bmi = HealthCalculatorUtils.calculateBMI(weight, height)
        val bmr = HealthCalculatorUtils.calculateBMR(gender, age, weight, height)
        val tdee = HealthCalculatorUtils.calculateTDEE(bmr, activityLevelPos)

        val profile = HealthProfileModel(
            userId = userId,
            gender = gender,
            age = age,
            weight = weight,
            height = height,
            activityLevel = activityLevelPos,
            bmi = bmi,
            bmr = bmr,
            tdee = tdee,
        )

        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.saveProfile(profile)
            if (success) {
                AppPreferences.setHealthProfileCompleted(context, true)
            }
            _saveStatus.value = success
            _isLoading.value = false
        }
    }

    fun resetStatus() {
        _saveStatus.value = null
    }
}