package com.hadat.aiyoga.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.firestore.model.MapPostModel
import com.hadat.aiyoga.databinding.FragmentMapBinding
import com.hadat.aiyoga.databinding.LayoutCustomMarkerBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.utils.singleClick

class MapFragment : BaseFragment<FragmentMapBinding, MapViewModel>(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var selectedLatLng: LatLng? = null
    private var selectedMarkerForOverlay: Marker? = null

    private val postsAdapter by lazy { MapPostsAdapter { } }


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

        binding.rvMarkerPosts.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = postsAdapter
        }

        binding.cvRadiusController.visibility = View.GONE
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.isBuildingsEnabled = true
        mMap?.uiSettings?.isTiltGesturesEnabled = true
        mMap?.apply {
            uiSettings.isMapToolbarEnabled = true
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = true

            setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
                override fun getInfoWindow(marker: Marker): View = View(requireContext()).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(1, 1)
                }
                override fun getInfoContents(marker: Marker): View? = null
            })

            setOnMarkerClickListener { marker ->
                val posts = (marker.tag as? List<*>)?.filterIsInstance<MapPostModel>().orEmpty()
                if (posts.isNotEmpty()) {
                    selectedMarkerForOverlay = marker
                    showPostsCarousel(posts)
                }
                false
            }

            setOnMapClickListener {
                binding.rvMarkerPosts.visibility = View.GONE
                selectedMarkerForOverlay = null
                setupNewLocationSelection(it)
            }

            setOnCameraMoveListener { updateOverlayPosition() }
        }

        ensureLocationPermissionAndEnable()
        viewModel.fetchLatestPosts()
    }

    private fun showPostsCarousel(posts: List<MapPostModel>) {
        postsAdapter.setList(posts)
        binding.rvMarkerPosts.visibility = View.VISIBLE
    }

    private fun updateOverlayPosition() {
        val marker = selectedMarkerForOverlay ?: return
        if (binding.rvMarkerPosts.visibility == View.VISIBLE) {
            val projection = mMap?.projection ?: return
            val screenPosition: Point = projection.toScreenLocation(marker.position)
            binding.rvMarkerPosts.x = (screenPosition.x - (binding.rvMarkerPosts.width / 2)).toFloat()
            binding.rvMarkerPosts.y = (screenPosition.y - binding.rvMarkerPosts.height - 50).toFloat()
        }
    }

    override fun initListener() {
        binding.btnShareLocation.singleClick {
            val latLng = selectedLatLng ?: return@singleClick
            mMap?.addMarker(MarkerOptions().position(latLng).icon(yogaMarkerIcon).anchor(0.5f, 1.0f))?.tag = emptyList<MapPostModel>()
            binding.btnShareLocation.visibility = View.GONE
            currentMarker?.remove()
        }
    }

    override fun initData() {
        viewModel.posts.observe(viewLifecycleOwner) { list ->
            mMap?.clear()
            list.groupBy { "${it.lat},${it.lng}" }.values.forEach { posts ->
                val first = posts.first()
                val marker = mMap?.addMarker(
                    MarkerOptions().position(LatLng(first.lat, first.lng)).icon(yogaMarkerIcon).anchor(0.5f, 1.0f)
                )
                marker?.tag = posts
            }
            moveToCurrentLocation()
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
        binding.btnShareLocation.visibility = View.VISIBLE
        binding.rvMarkerPosts.visibility = View.GONE
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                val cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(18f)
                    .tilt(60f)
                    .bearing(30f)
                    .build()

                mMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }
}