package com.hadat.aiyoga.information

import android.widget.ArrayAdapter
import android.widget.Toast
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentInformationBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate

class InformationFragment : BaseFragment<FragmentInformationBinding, InformationViewModel>() {

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
        val age = binding.edtAge.text.toString()
        val weight = binding.edtWeight.text.toString()
        val height = binding.edtHeight.text.toString()

        if (age.isBlank() || weight.isBlank() || height.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.msg_fill_all), Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveHealthProfile(
            context = requireContext(),
            gender = binding.spGender.selectedItem.toString(),
            age = age.toInt(),
            weight = weight.toFloat(),
            height = height.toFloat(),
            activityLevelPos = binding.spActivity.selectedItemPosition
        )
    }
}