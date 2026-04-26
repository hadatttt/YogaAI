package com.hadat.aiyoga.profile

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.hadat.aiyoga.MainActivity
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentProfileBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.utils.service.NotificationHelper
import com.hadat.aiyoga.utils.service.NotificationWorker
import com.hadat.aiyoga.utils.view.CloudinaryUtils
import com.hadat.aiyoga.utils.view.ViewUtils.removeVietnameseAccents
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigateWithPopAll
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
                saveProfile()
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
        renderLanguage()
    }

    override fun initData() {
        val userId = AppPreferences.getUserId(requireContext()) ?: return
        viewModel.loadUser(userId)
        viewModel.loadHealthProfile(userId)
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            binding.edtDisplayName.setText(user.displayName.removeVietnameseAccents())
            currentPhotoUrl = user.photoUrl
            binding.imgAvatar.loadImageFromNetwork(user.photoUrl)
        }
        viewModel.healthProfileData.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.tvHeightValue.text = "${it.height.toInt()} cm"
                binding.tvWeightValue.text = "${it.weight.toInt()} kg"
            }
        }
        viewModel.saveStatus.observe(viewLifecycleOwner) { ok ->
            if (ok == null) return@observe
            binding.pbSaving.visibility = android.view.View.GONE
            showToast(if (ok) getString(R.string.profile_updated) else getString(R.string.update_failed))
            viewModel.resetSaveStatus()
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick {
            binding.edtDisplayName.clearFocus()
            popBackStack()
        }
        binding.edtDisplayName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveProfile()
            }
        }
        binding.ivEditAvatar.singleClick { pickImageLauncher.launch("image/*") }
        binding.layoutNotificationTime.singleClick { showTimePicker() }
        binding.layoutLanguage.singleClick { showLanguagePicker() }
        binding.layoutBodyStats.singleClick {
            navigateWithPopAll(R.id.informationFragment)
        }


        binding.btnLogout.singleClick {
            viewModel.logout(requireContext()) {
                AppPreferences.logout(requireContext())

                val intent = android.content.Intent(requireContext(), MainActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
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
            .setTitleText(getString(R.string.select_reminder_time))
            .setTheme(R.style.CustomTimePickerTheme)
            .build()

        picker.show(childFragmentManager, "MATERIAL_TIME_PICKER")

        picker.addOnPositiveButtonClickListener {
            reminderTime = String.format("%02d:%02d", picker.hour, picker.minute)
            binding.tvNotificationTime.text = reminderTime
            AppPreferences.setNotificationTime(requireContext(), reminderTime)
            NotificationHelper.checkPermission(this, onGranted = {
                NotificationWorker.scheduleDailyNotifications(requireContext(), listOf(reminderTime))
            }, permissionLauncher = notificationPermissionLauncher)

            saveProfile()
        }
    }
    private fun saveProfile() {
        val userId = AppPreferences.getUserId(requireContext()) ?: return
        val newName = binding.edtDisplayName.text?.toString()?.trim().orEmpty().removeVietnameseAccents()

        if (newName.isBlank()) return
        binding.pbSaving.visibility = android.view.View.VISIBLE

        selectedImageUri?.let { uri ->
            CloudinaryUtils.uploadImage(
                context = requireContext(),
                imageUri = uri,
                onSuccess = { url ->
                    currentPhotoUrl = url
                    selectedImageUri = null
                    viewModel.updateProfile(userId, newName, url)
                },
                onError = {
                    showToast(it)
                    binding.pbSaving.visibility = android.view.View.GONE
                }
            )
        } ?: viewModel.updateProfile(userId, newName, currentPhotoUrl)
    }

    private fun showLanguagePicker() {
        val currentLanguage = AppPreferences.getLanguageCode(requireContext())
        val languageCodes = arrayOf("en", "vi")
        val languageLabels = arrayOf(
            getString(R.string.english),
            getString(R.string.vietnamese)
        )
        val checkedIndex = languageCodes.indexOf(currentLanguage).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_language))
            .setSingleChoiceItems(languageLabels, checkedIndex) { dialog, which ->
                val selectedCode = languageCodes[which]
                AppPreferences.setLanguageCode(requireContext(), selectedCode)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedCode))
                renderLanguage()
                dialog.dismiss()
                requireActivity().recreate()
            }
            .setNegativeButton(getString(R.string.title_cancel), null)
            .show()
    }

    private fun renderLanguage() {
        binding.tvLanguageValue.text = when (AppPreferences.getLanguageCode(requireContext())) {
            "vi" -> getString(R.string.vietnamese)
            else -> getString(R.string.english)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

