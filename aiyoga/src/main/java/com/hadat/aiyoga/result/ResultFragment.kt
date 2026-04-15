package com.hadat.aiyoga.result

import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.DialogSharePlaceBinding
import com.hadat.aiyoga.databinding.FragmentResultBinding
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
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
        args.workoutResultList?.firstOrNull()?.let { bindWorkoutData(it) }

        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        viewModel.fetchWorkoutHistory(userId)
        viewModel.loadCurrentUser(userId)
        viewModel.workoutHistory.observe(viewLifecycleOwner) { historyAdapter.setList(it) }

        val captured = args.workoutResultList?.firstOrNull()?.capturedImages.orEmpty()
        capturedAdapter.setList(captured)

        fetchLastLocation()

        viewModel.shareStatus.observe(viewLifecycleOwner) { ok ->
            if (ok == null) return@observe
            showToast(if (ok) "Shared to map successfully!" else "Share failed")
            viewModel.resetShareStatus()
        }
    }

    private fun bindWorkoutData(data: WorkoutResultModel) {
        val animationDuration = 1500L

        val caloriesBurned = data.durationInSeconds * 0.15f
        val maxCaloriesGoal = 30f
        binding.progressCalories.apply {
            progressMax = maxCaloriesGoal
            setProgressWithAnimation(caloriesBurned, animationDuration)
        }
        binding.tvCaloriesValue.text = String.format("%.1f", caloriesBurned)
        binding.tvCaloriesLabel.text = "of ${maxCaloriesGoal.toInt()} kcal"

        val targetSeconds = 60f
        binding.progressTime.apply {
            progressMax = targetSeconds
            setProgressWithAnimation(data.durationInSeconds.toFloat(), animationDuration)
        }
        binding.tvTimeValue.text = "${data.durationInSeconds}s"

        val accuracyPercent = (100f - (data.errorCount * 5f)).coerceIn(10f, 100f)
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
        val captured = args.workoutResultList?.firstOrNull()?.capturedImages.orEmpty()
        if (captured.isEmpty()) {
            showToast("No photos available to share")
            return
        }

        val dialogBinding = DialogSharePlaceBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = android.app.Dialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            window?.setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val selectableAdapter = CapturedImagesAdapter(selectable = true)
        dialogBinding.rvImages.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = selectableAdapter
        }
        selectableAdapter.setList(captured)
        selectableAdapter.setSelected(listOf(captured.first()))

        updateLocationStatus(dialogBinding)

        dialogBinding.btnCancel.singleClick { dialog.dismiss() }
        dialogBinding.btnShare.singleClick {
            val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
            val lat = lastLat
            val lng = lastLng
            if (lat == null || lng == null) {
                requestLocationPermission.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                updateLocationStatus(dialogBinding)
                return@singleClick
            }

            val desc = dialogBinding.edtDescription.text?.toString().orEmpty().trim()
            val selectedImages = selectableAdapter.getSelected()
            val workout = args.workoutResultList?.firstOrNull()

            viewModel.sharePlace(
                context = requireContext(),
                userId = userId,
                userName = viewModel.currentUser.value?.displayName ?: userId,
                userAvatar = viewModel.currentUser.value?.photoUrl.orEmpty(),
                description = desc,
                imageUris = selectedImages,
                lat = lat,
                lng = lng,
                workout = workout
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