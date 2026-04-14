package com.hadat.aiyoga.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.view.View
import android.widget.SeekBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentMapBinding
import com.hadat.aiyoga.databinding.LayoutCustomMarkerBinding
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.utils.singleClick

class MapFragment : BaseFragment<FragmentMapBinding, MapViewModel>(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var currentCircle: Circle? = null
    private var currentMarker: Marker? = null
    private var selectedLatLng: LatLng? = null

    private var selectedMarkerForOverlay: Marker? = null

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
        mMap?.apply {
            uiSettings.isMapToolbarEnabled = true
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = true

            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(16.0544, 108.2022), 15f))
            setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
                override fun getInfoWindow(marker: Marker): View {
                    return View(requireContext()).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(1, 1)
                    }
                }
                override fun getInfoContents(marker: Marker): View? = null
            })

            setOnMarkerClickListener { marker ->
                val place = marker.tag as? ZenPlace
                if (place != null) {
                    selectedMarkerForOverlay = marker
                    showCustomOverlay(marker, place)
                }
                false
            }

            setOnMapClickListener {
                binding.layoutOverlayInfo.root.visibility = View.GONE
                selectedMarkerForOverlay = null
            }

            setOnCameraMoveListener {
                updateOverlayPosition()
            }
        }

        mMap?.setOnMapLongClickListener { setupNewZenSelection(it) }
        viewModel.fetchAllZenPlaces()
    }

    private fun showCustomOverlay(marker: Marker, place: ZenPlace) {
        // layoutOverlayInfo là cái <include> layout_custom_info_window trong fragment_map.xml
        binding.layoutOverlayInfo.apply {
            tvName.text = place.creatorName
            tvTime.text = place.time
            tvDescription.text = place.description

            // Sử dụng hàm load ảnh từ internet của bạn
            imgAvatar.loadImageFromNetwork(place.creatorAvatar)
            imgBackground.loadImageFromNetwork(place.creatorAvatar)

            root.visibility = View.VISIBLE
            // Ép đo lường lại kích thước để tính toán vị trí chính xác
            root.post { updateOverlayPosition() }
        }
    }

    private fun updateOverlayPosition() {
        val marker = selectedMarkerForOverlay ?: return
        if (binding.layoutOverlayInfo.root.visibility == View.VISIBLE) {
            val projection = mMap?.projection ?: return
            val screenPosition: Point = projection.toScreenLocation(marker.position)

            // Tính toán để Overlay nằm chính giữa Marker và cách lên trên một khoảng
            val x = screenPosition.x - (binding.layoutOverlayInfo.root.width / 2)
            val y = screenPosition.y - binding.layoutOverlayInfo.root.height - 50 // -50 là offset khoảng cách

            binding.layoutOverlayInfo.root.x = x.toFloat()
            binding.layoutOverlayInfo.root.y = y.toFloat()
        }
    }

    override fun initListener() {
        binding.sbRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                val radius = p.toDouble().coerceAtLeast(10.0)
                binding.tvRadius.text = "Bán kính vùng Zen: ${radius.toInt()}m"
                currentCircle?.radius = radius
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.btnShareLocation.singleClick {
            val latLng = selectedLatLng ?: return@singleClick
            val radius = currentCircle?.radius ?: 50.0
            val imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT3s7xQrKz46dWK_UZ0J5UWVbnjxtWVp3nEYQ&s"

            val marker = mMap?.addMarker(MarkerOptions().position(latLng).icon(yogaMarkerIcon).anchor(0.5f, 1.0f))
            marker?.tag = ZenPlace(
                id = "me", name = "Vị trí của tôi", creatorName = "Khánh Đạt",
                creatorAvatar = imageUrl, backgroundImage = imageUrl,
                description = "Vừa mới check-in địa điểm thiền định mới!",
                time = "12/04/2026", lat = latLng.latitude, lng = latLng.longitude, radius = radius
            )

            binding.cvRadiusController.visibility = View.GONE
            binding.btnShareLocation.visibility = View.GONE
            currentMarker?.remove()
            currentCircle?.remove()
        }
    }

    override fun initData() {
        viewModel.zenPlaces.observe(viewLifecycleOwner) { list ->
            mMap?.clear()
            list?.forEach { place ->
                val pos = LatLng(place.lat, place.lng)
                mMap?.addCircle(CircleOptions().center(pos).radius(place.radius).fillColor(Color.parseColor("#302ECC71")).strokeWidth(0f))
                val marker = mMap?.addMarker(MarkerOptions().position(pos).icon(yogaMarkerIcon).anchor(0.5f, 1.0f))
                marker?.tag = place
            }
        }
    }

    private fun setupNewZenSelection(latLng: LatLng) {
        selectedLatLng = latLng
        currentMarker?.remove()
        currentCircle?.remove()
        currentMarker = mMap?.addMarker(MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        currentCircle = mMap?.addCircle(CircleOptions().center(latLng).radius(50.0).fillColor(Color.parseColor("#402ECC71")).strokeColor(Color.parseColor("#2ECC71")).strokeWidth(2f))
        binding.cvRadiusController.visibility = View.VISIBLE
        binding.btnShareLocation.visibility = View.VISIBLE
        binding.layoutOverlayInfo.root.visibility = View.GONE
    }
}