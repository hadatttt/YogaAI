package com.hadat.aiyoga.shareplace

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentSharePlaceBinding
import com.hadat.aiyoga.result.CapturedImagesAdapter
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.yalantis.ucrop.UCrop
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.io.File
class SharePlaceFragment : BaseFragment<FragmentSharePlaceBinding, SharePlaceViewModel>() {

    private val args by navArgs<SharePlaceFragmentArgs>()

    private val capturedAdapter by lazy {
        CapturedImagesAdapter(selectable = true) { selectedUri ->
            viewModel.selectImage(selectedUri)
        }
    }

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(requireActivity()) }
    private var lastLat: Double? = null
    private var lastLng: Double? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startCrop(it) }
    }

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                viewModel.selectImage(it.toString())
            }
        }
    }

    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLastLocation { executeShare() }
        else showToast(getString(R.string.location_not_granted))
    }

    override fun initView() {
        binding.rvImages.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = capturedAdapter
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

        binding.ivCrop.singleClick {
            viewModel.selectedImage.value?.let { startCrop(Uri.parse(it)) }
        }

        binding.ivCameraCapture.singleClick {
            pickImageLauncher.launch("image/*")
        }

        binding.btnShare.singleClick {
            checkPermissionAndShare()
        }
    }

    override fun initData() {
        val userId = AppPreferences.getUserId(requireContext()) ?: ""
        val images = args.capturedImagesList?.toList().orEmpty().filter { it.isNotBlank() }

        viewModel.initData(images, userId)

        viewModel.capturedImages.observe(viewLifecycleOwner) { list ->
            capturedAdapter.setList(list)
        }

        viewModel.selectedImage.observe(viewLifecycleOwner) { uri ->
            binding.imgYogaPose.loadImageFromNetwork(uri)
            val imageList = viewModel.capturedImages.value ?: emptyList()
            if (imageList.contains(uri)) {
                capturedAdapter.setSelected(uri)
            } else {
                capturedAdapter.clearSelection()
            }
        }

        viewModel.shareStatus.observe(viewLifecycleOwner) { ok ->
            if (ok == null) return@observe
            if (ok) {
                showToast(getString(R.string.shared_to_map_successfully))
                popBackStack()
            } else {
                showToast(getString(R.string.share_failed))
            }
            viewModel.resetShareStatus()
        }

        fetchLastLocation()
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "share_crop_${System.currentTimeMillis()}.jpg"))
        val intent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .getIntent(requireContext())
        cropImageLauncher.launch(intent)
    }

    private fun checkPermissionAndShare() {
        if (lastLat != null && lastLng != null) executeShare()
        else {
            requestLocationPermission.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun executeShare() {
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        val desc = binding.edtDescription.text.toString().trim()
        viewModel.sharePlace(requireContext(), userId, desc, lastLat ?: 0.0, lastLng ?: 0.0)
    }

    private fun fetchLastLocation(onSuccess: (() -> Unit)? = null) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    lastLat = it.latitude
                    lastLng = it.longitude
                    onSuccess?.invoke()
                }
            }
        } catch (e: SecurityException) { }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}