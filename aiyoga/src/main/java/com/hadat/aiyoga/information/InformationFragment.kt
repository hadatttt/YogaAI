package com.hadat.aiyoga.information

import android.widget.ArrayAdapter
import android.widget.Toast
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentInformationBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate

class InformationFragment : BaseFragment<FragmentInformationBinding, InformationViewModel>() {
    companion object {
        private const val MIN_AGE = 10
        private const val MAX_AGE = 100
        private const val MIN_WEIGHT = 20f
        private const val MAX_WEIGHT = 250f
        private const val MIN_HEIGHT = 50f
        private const val MAX_HEIGHT = 250f
    }
    private val genders by lazy {
        listOf(
            getString(R.string.gender_male),
            getString(R.string.gender_female),
            getString(R.string.gender_other)
        )
    }

    private val activityLevels by lazy {
        listOf(
            getString(R.string.activity_sedentary),
            getString(R.string.activity_lightly),
            getString(R.string.activity_moderately),
            getString(R.string.activity_very)
        )
    }

    override fun initView() {
        setupSpinners()
    }

    override fun initListener() {
        binding.btnSaveProfile.setOnClickListener {
            validateAndSave()
        }
    }

    override fun initData() {
        viewModel.fetchProfile()

        viewModel.profileData.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.edtAge.setText(it.age.toString())
                binding.edtWeight.setText(it.weight.toString())
                binding.edtHeight.setText(it.height.toString())

                val genderIndex = genders.indexOf(it.gender)
                if (genderIndex >= 0) binding.spGender.setSelection(genderIndex)

                if (it.activityLevel in activityLevels.indices) {
                    binding.spActivity.setSelection(it.activityLevel)
                }
            }
        }

        viewModel.saveStatus.observe(viewLifecycleOwner) { success ->
            when (success) {
                true -> {
                    Toast.makeText(requireContext(), getString(R.string.msg_save_success), Toast.LENGTH_SHORT).show()
                    viewModel.resetStatus()
                    navigate(R.id.homeFragment)
                }
                false -> {
                    Toast.makeText(requireContext(), getString(R.string.msg_save_failed), Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnSaveProfile.isEnabled = !loading
            binding.btnSaveProfile.text = if (loading) {
                getString(R.string.btn_calculating)
            } else {
                getString(R.string.btn_save)
            }
        }
    }

    private fun setupSpinners() {
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders)
        binding.spGender.adapter = genderAdapter

        val activityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, activityLevels)
        binding.spActivity.adapter = activityAdapter
    }

    private fun validateAndSave() {
        val ageStr = binding.edtAge.text.toString().trim()
        val weightStr = binding.edtWeight.text.toString().trim()
        val heightStr = binding.edtHeight.text.toString().trim()

        if (ageStr.isBlank() || weightStr.isBlank() || heightStr.isBlank()) {
            showToast(getString(R.string.msg_fill_all))
            return
        }
        val age = ageStr.toIntOrNull() ?: 0
        val weight = weightStr.toFloatOrNull() ?: 0f
        val height = heightStr.toFloatOrNull() ?: 0f

        when {
            age !in MIN_AGE..MAX_AGE -> {
                showToast(getString(R.string.error_invalid_age))
                binding.edtAge.requestFocus()
            }
            weight !in MIN_WEIGHT..MAX_WEIGHT -> {
                showToast(getString(R.string.error_invalid_weight))
                binding.edtWeight.requestFocus()
            }
            height !in MIN_HEIGHT..MAX_HEIGHT -> {
                showToast(getString(R.string.error_invalid_height))
                binding.edtHeight.requestFocus()
            }
            else -> {
                viewModel.saveHealthProfile(
                    context = requireContext(),
                    gender = binding.spGender.selectedItem.toString(),
                    age = age,
                    weight = weight,
                    height = height,
                    activityLevelPos = binding.spActivity.selectedItemPosition
                )
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}