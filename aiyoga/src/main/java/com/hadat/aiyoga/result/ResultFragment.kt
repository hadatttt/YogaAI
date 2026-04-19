package com.hadat.aiyoga.result

import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.firestore.model.HealthProfileModel
import com.hadat.aiyoga.databinding.DialogSharePlaceBinding
import com.hadat.aiyoga.databinding.FragmentResultBinding
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
import com.hadat.aiyoga.utils.yogautils.HealthCalculatorUtils
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ResultFragment : BaseFragment<FragmentResultBinding, ResultViewModel>() {

    private val args by navArgs<ResultFragmentArgs>()
    private val historyAdapter by lazy { WorkoutHistoryAdapter { } }
    private val capturedAdapter by lazy { CapturedImagesAdapter(selectable = false) }

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(requireActivity()) }
    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var healthProfile: HealthProfileModel? = null

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val ok = (result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true)
            if (ok) fetchLastLocation()
        }

    override fun initView() {
        binding.rvWorkoutHistory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = historyAdapter
        }
        binding.rvCapturedImages.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = capturedAdapter
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack(R.id.homeFragment) }
        binding.btnSharePlace.singleClick { openShareDialog() }
    }

    override fun initData() {
        val resultList = args.workoutResultList?.toList().orEmpty()

        if (resultList.isNotEmpty()) {
            bindWorkoutSummary(resultList)
            historyAdapter.setList(resultList)
            val mergedImages = resultList.flatMap { it.capturedImages }.distinct()
            capturedAdapter.setList(mergedImages)
        }
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        if (resultList.isNotEmpty()) {
            viewModel.saveWorkoutResults(resultList, userId)
        }
        viewModel.loadHealthProfile(userId)
        viewModel.fetchWorkoutHistory(userId)
        viewModel.loadCurrentUser(userId)
        viewModel.healthProfile.observe(viewLifecycleOwner) {
            healthProfile = it
        }
        viewModel.workoutHistory.observe(viewLifecycleOwner) {
            if (resultList.isEmpty()) historyAdapter.setList(it)
        }

        fetchLastLocation()

        viewModel.shareStatus.observe(viewLifecycleOwner) { ok ->
            if (ok == null) return@observe
            showToast(
                if (ok) getString(R.string.shared_to_map_successfully)
                else getString(R.string.share_failed)
            )
            viewModel.resetShareStatus()
        }
    }

    private fun bindWorkoutSummary(list: List<WorkoutResultModel>) {

        val animationDuration = 1500L

        val totalSeconds = list.sumOf { it.durationInSeconds }
        val totalError = list.sumOf { it.errorCount }

        val profile = healthProfile

        val tdee = profile?.tdee ?: 2000f


        val weight = healthProfile?.weight ?: 60f

        HealthCalculatorUtils.calculateTotalCalories(
            workouts = list,
            weight = weight,
            onMet = { id, callback ->
                YogaDataUtils.getRemoteYogaMet(id, callback)
            }
        ) { totalCalories ->

            binding.progressCalories.apply {
                progressMax = healthProfile?.tdee ?: 2000f
                setProgressWithAnimation(totalCalories, 1500L)
            }

            binding.tvCaloriesValue.text =
                String.format("%.1f", totalCalories)
            binding.tvCaloriesLabel.text =
                "of ${healthProfile?.tdee?.toInt() ?: 2000} kcal "
        }
        val targetSeconds = 60f

        binding.progressTime.apply {
            progressMax = targetSeconds
            setProgressWithAnimation(totalSeconds.toFloat(), animationDuration)
        }

        binding.tvTimeValue.text = "${totalSeconds}s"



        val accuracyPercent = HealthCalculatorUtils.calculateAccuracy(totalSeconds,totalError)

        binding.progressAccuracy.apply {
            progressMax = 100f
            progressBarColor = when {
                accuracyPercent >= 85 -> Color.parseColor("#00C853")
                accuracyPercent >= 60 -> Color.parseColor("#FFD600")
                else -> Color.parseColor("#FF6A00")
            }
            setProgressWithAnimation(accuracyPercent, animationDuration)
        }

        binding.tvAccuracyValue.text = "${accuracyPercent.toInt()}%"
    }


    private fun openShareDialog() {
        val captured = args.workoutResultList?.toList().orEmpty()
            .flatMap { it.capturedImages }
            .filter { it.isNotBlank() }
            .distinct()

        if (captured.isEmpty()) {
            showToast(getString(R.string.no_image_to_share))
            return
        }

        val dialogBinding = DialogSharePlaceBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = android.app.Dialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val selectableAdapter = CapturedImagesAdapter(
            selectable = true,
            onSelectionChanged = { selectedList ->
            }
        ).apply {
            setList(captured)
            setSelected(listOf(captured.first()))
        }

        dialogBinding.rvImages.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = selectableAdapter
        }

        updateLocationStatus(dialogBinding)

        dialogBinding.btnCancel.singleClick { dialog.dismiss() }

        dialogBinding.btnShare.singleClick {
            val lat = lastLat
            val lng = lastLng
            if (lat == null || lng == null) {
                requestLocationPermission.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                showToast(getString(R.string.request_location_permission_share))
                updateLocationStatus(dialogBinding)
                return@singleClick
            }
            val selectedImage = selectableAdapter.getSelected().firstOrNull()
            if (selectedImage == null) {
                showToast(getString(R.string.please_select_best_photo))
                return@singleClick
            }
            val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
            val desc = dialogBinding.edtDescription.text?.toString().orEmpty().trim()
            viewModel.sharePlace(
                context = requireContext(),
                userId = userId,
                userName = viewModel.currentUser.value?.displayName ?: userId,
                userAvatar = viewModel.currentUser.value?.photoUrl.orEmpty(),
                description = desc,
                imageUri = selectedImage,
                lat = lat,
                lng = lng
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateLocationStatus(dialogBinding: DialogSharePlaceBinding) {
        val lat = lastLat
        val lng = lastLng
        dialogBinding.tvLocationStatus.text =
            if (lat != null && lng != null) "Location: $lat, $lng"
            else "Location: permission not granted / unavailable"
    }

    private fun fetchLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    lastLat = it.latitude
                    lastLng = it.longitude
                }
            }
        } catch (_: SecurityException) {
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}