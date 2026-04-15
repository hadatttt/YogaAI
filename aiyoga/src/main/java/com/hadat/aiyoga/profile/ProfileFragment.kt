package com.hadat.aiyoga.profile

import android.app.TimePickerDialog
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentProfileBinding
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.service.NotificationHelper
import com.hadat.aiyoga.service.NotificationWorker
import com.hadat.aiyoga.utils.CloudinaryUtils
import com.hadat.aiyoga.utils.ViewUtils.removeVietnameseAccents
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {

    private var selectedImageUri: Uri? = null
    private var currentPhotoUrl: String = ""
    private var reminderTime: String = "19:00"

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                binding.imgAvatar.setImageURI(it)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted && Build.VERSION.SDK_INT >= 33) {
                NotificationHelper.showSettingsDialog(requireActivity())
            }
        }

    override fun initView() {
        reminderTime = AppPreferences.getNotificationTime(requireContext())
        binding.tvNotificationTime.text = reminderTime
    }

    override fun initData() {
        val userId = AppPreferences.getUserId(requireContext()) ?: return
        viewModel.loadUser(userId)

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            binding.edtDisplayName.setText(user.displayName.removeVietnameseAccents())
            currentPhotoUrl = user.photoUrl
            binding.imgAvatar.loadImageFromNetwork(user.photoUrl)
        }

        viewModel.saveStatus.observe(viewLifecycleOwner) { ok ->
            if (ok == null) return@observe
            showToast(if (ok) "Profile updated" else "Update failed")
            viewModel.resetSaveStatus()
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.ivEditAvatar.singleClick { pickImageLauncher.launch("image/*") }
        binding.layoutNotificationTime.singleClick { showTimePicker() }

        binding.ivSave.singleClick {
            NotificationHelper.checkPermission(this, onGranted = {
                NotificationWorker.scheduleDailyNotifications(requireContext(), listOf(reminderTime))
            }, permissionLauncher = notificationPermissionLauncher)

            AppPreferences.setNotificationTime(requireContext(), reminderTime)
            saveProfile()
        }

        binding.btnLogout.singleClick {
            viewModel.logout(requireContext()) {
                AppPreferences.logout(requireContext())
                navigate(R.id.loginFragment, isPop = true)
            }
        }
    }



    private fun showTimePicker() {
        val currentHour = reminderTime.substringBefore(":").toIntOrNull() ?: 19
        val currentMinute = reminderTime.substringAfter(":").toIntOrNull() ?: 0

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Select Reminder Time")
            .setTheme(R.style.CustomTimePickerTheme)
            .build()

        picker.show(childFragmentManager, "MATERIAL_TIME_PICKER")

        picker.addOnPositiveButtonClickListener {
            reminderTime = String.format("%02d:%02d", picker.hour, picker.minute)
            binding.tvNotificationTime.text = reminderTime
        }
    }
    private fun saveProfile() {
        val userId = AppPreferences.getUserId(requireContext()) ?: return
        val newName = binding.edtDisplayName.text?.toString()?.trim().orEmpty().removeVietnameseAccents()
        if (newName.isBlank()) {
            showToast("Name cannot be empty")
            return
        }

        selectedImageUri?.let { uri ->
            CloudinaryUtils.uploadImage(
                context = requireContext(),
                imageUri = uri,
                onSuccess = { url ->
                    currentPhotoUrl = url
                    viewModel.updateProfile(userId, newName, url)
                },
                onError = { showToast(it) }
            )
        } ?: viewModel.updateProfile(userId, newName, currentPhotoUrl)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

