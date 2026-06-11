package com.hadat.aiyoga.map

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.firestore.model.MapPostModel
import com.hadat.aiyoga.databinding.DialogDeleteBinding
import com.hadat.aiyoga.databinding.FragmentMapBinding
import com.hadat.aiyoga.databinding.LayoutCustomMarkerBinding
import com.hadat.aiyoga.databinding.LayoutMapPostsBottomSheetBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.utils.singleClick
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapFragment : BaseFragment<FragmentMapBinding, MapViewModel>(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var selectedLatLng: LatLng? = null
    private var loadVisiblePostsJob: Job? = null
    private var shouldOpenMyReviewsSheet = false
    private var myReviewsSheet: BottomSheetDialog? = null

    private val myReviewsAdapter by lazy {
        MapPostsAdapter(
            showDelete = true,
            onDeleteClick = { post -> showDeleteDialog(post) },
            onItemClick = { post -> showPostOnMap(post) }
        )
    }

    private companion object {
        const val MAP_POSTS_DEBOUNCE_MS = 500L
        const val MIN_POSTS_ZOOM = 10f
    }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) enableUserLocation()
        }

    private val yogaMarkerIcon: BitmapDescriptor by lazy {
        val markerBinding = LayoutCustomMarkerBinding.inflate(layoutInflater)
        val view = markerBinding.root
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun initView() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMapSettings()
        setupMapListeners()
        ensureLocationPermissionAndEnable()
    }

    private fun setupMapSettings() {
        mMap?.apply {
            isBuildingsEnabled = true
            uiSettings.apply {
                isTiltGesturesEnabled = true
                isMapToolbarEnabled = true
                isZoomControlsEnabled = true
                isMyLocationButtonEnabled = true
            }
        }
    }

    private fun setupMapListeners() {
        mMap?.apply {
            setOnMarkerClickListener { marker ->
                val posts = (marker.tag as? List<*>)?.filterIsInstance<MapPostModel>().orEmpty()
                if (posts.isNotEmpty()) {
                    showPostsBottomSheet(posts, marker.position)
                    return@setOnMarkerClickListener true
                }
                false
            }

            setOnMapClickListener { latLng ->
                setupNewLocationSelection(latLng)
            }

            setOnCameraIdleListener {
                scheduleVisiblePostsFetch()
            }
        }
    }

    private fun scheduleVisiblePostsFetch() {
        loadVisiblePostsJob?.cancel()
        loadVisiblePostsJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(MAP_POSTS_DEBOUNCE_MS)
            fetchPostsForVisibleRegion()
        }
    }

    private fun fetchPostsForVisibleRegion() {
        val map = mMap ?: return
        if (map.cameraPosition.zoom < MIN_POSTS_ZOOM) {
            viewModel.clearPosts()
            return
        }

        val bounds = map.projection.visibleRegion.latLngBounds
        viewModel.fetchPostsInBounds(
            southLat = bounds.southwest.latitude,
            northLat = bounds.northeast.latitude,
            westLng = bounds.southwest.longitude,
            eastLng = bounds.northeast.longitude
        )
    }

    private fun showPostsBottomSheet(posts: List<MapPostModel>, location: LatLng) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetBinding = LayoutMapPostsBottomSheetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val postsAdapter = MapPostsAdapter { /* Handle post click if needed */ }
        sheetBinding.rvMarkerPosts.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = postsAdapter
        }
        postsAdapter.setList(posts.sortedByDescending { it.createdAt })

        sheetBinding.btnDirection.singleClick {
            val uri = Uri.parse("google.navigation:q=${location.latitude},${location.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            startActivity(intent)
        }

        sheetBinding.btnShare.singleClick {
            sheetBinding.btnShare.isEnabled = false
            val lat = location.latitude
            val lng = location.longitude
            val mapLink = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
            val shareBody = getString(R.string.map_share_location_body, mapLink)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareBody)
            }
            val chooser = Intent.createChooser(shareIntent, getString(R.string.map_share_location_chooser_title))
            startActivity(chooser)
            dialog.dismiss()
            sheetBinding.btnShare.postDelayed({ sheetBinding.btnShare.isEnabled = true }, 500)
        }

        dialog.show()
    }

    override fun initData() {
        viewModel.posts.observe(viewLifecycleOwner) { allPosts ->
            mMap?.clear()
            clusterPosts(allPosts).forEach { (center, posts) ->
                val marker = mMap?.addMarker(
                    MarkerOptions()
                        .position(center)
                        .icon(yogaMarkerIcon)
                        .anchor(0.5f, 1.0f)
                )
                marker?.tag = posts
            }
        }

        viewModel.myPosts.observe(viewLifecycleOwner) { posts ->
            myReviewsAdapter.setList(posts)
            if (shouldOpenMyReviewsSheet) {
                shouldOpenMyReviewsSheet = false
                if (posts.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.no_map_reviews), Toast.LENGTH_SHORT).show()
                } else {
                    showMyReviewsBottomSheet(posts)
                }
            } else if (myReviewsSheet?.isShowing == true && posts.isEmpty()) {
                myReviewsSheet?.dismiss()
            }
        }

        viewModel.deleteStatus.observe(viewLifecycleOwner) { ok ->
            if (ok == null) return@observe
            Toast.makeText(
                requireContext(),
                getString(if (ok) R.string.delete_map_review_success else R.string.delete_map_review_failed),
                Toast.LENGTH_SHORT
            ).show()
            viewModel.resetDeleteStatus()
            fetchPostsForVisibleRegion()
        }
    }

    private fun setupNewLocationSelection(latLng: LatLng) {
        selectedLatLng = latLng
        currentMarker?.remove()
        currentMarker = mMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
    }

    private fun ensureLocationPermissionAndEnable() {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) enableUserLocation()
        else requestLocationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun enableUserLocation() {
        try {
            mMap?.isMyLocationEnabled = true
            moveToCurrentLocation()
        } catch (_: SecurityException) { }
    }

    private fun moveToCurrentLocation() {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return

        try {
            LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)

                    val cameraPosition = CameraPosition.Builder()
                        .target(latLng)
                        .zoom(16f)
                        .tilt(45f)
                        .bearing(30f)
                        .build()

                    mMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
            }
        } catch (_: SecurityException) { }
    }

    private fun clusterPosts(posts: List<MapPostModel>): Map<LatLng, List<MapPostModel>> {
        val clusters = mutableListOf<MutableList<MapPostModel>>()
        val radiusInMeters = 50.0

        for (post in posts) {
            var added = false
            for (cluster in clusters) {
                val centerLat = cluster.map { it.lat }.average()
                val centerLng = cluster.map { it.lng }.average()
                val results = FloatArray(1)
                android.location.Location.distanceBetween(post.lat, post.lng, centerLat, centerLng, results)

                if (results[0] <= radiusInMeters) {
                    cluster.add(post)
                    added = true
                    break
                }
            }
            if (!added) clusters.add(mutableListOf(post))
        }

        return clusters.associate { list ->
            LatLng(list.map { it.lat }.average(), list.map { it.lng }.average()) to list
        }
    }

    override fun initListener() {
        binding.btnMyReviews.singleClick {
            shouldOpenMyReviewsSheet = true
            fetchMyReviews()
        }
    }

    private fun fetchMyReviews() {
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        viewModel.fetchMyPosts(userId)
    }

    private fun showMyReviewsBottomSheet(posts: List<MapPostModel>) {
        myReviewsSheet?.dismiss()
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetBinding = LayoutMapPostsBottomSheetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        myReviewsSheet = dialog

        sheetBinding.tvLocationTitle.text = getString(R.string.my_map_reviews)
        sheetBinding.rvMarkerPosts.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = myReviewsAdapter
        }
        myReviewsAdapter.setList(posts)
        sheetBinding.layoutActions.visibility = View.GONE

        dialog.setOnDismissListener {
            if (myReviewsSheet == dialog) myReviewsSheet = null
        }
        dialog.show()
    }

    private fun showPostOnMap(post: MapPostModel) {
        myReviewsSheet?.dismiss()
        val latLng = LatLng(post.lat, post.lng)
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        showPostsBottomSheet(listOf(post), latLng)
    }

    private fun showDeleteDialog(post: MapPostModel) {
        val dialog = Dialog(requireContext())
        val dialogBinding = DialogDeleteBinding.inflate(layoutInflater)

        dialog.apply {
            setContentView(dialogBinding.root)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout((resources.displayMetrics.widthPixels * 0.86).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }

        dialogBinding.ivClose.singleClick { dialog.dismiss() }
        dialogBinding.btnConfirm.singleClick {
            val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
            viewModel.deleteMyPost(post, userId)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        loadVisiblePostsJob?.cancel()
        loadVisiblePostsJob = null
        super.onDestroyView()
    }
}
