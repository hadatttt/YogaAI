package com.hadat.aiyoga.yoga_ai


import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.airbnb.lottie.LottieAnimationView
import com.github.chrisbanes.photoview.PhotoView
import com.hadat.aiyoga.R
import com.hadat.aiyoga.utils.view.loadImageFromNetwork

class DialogZoom : DialogFragment() {

    private var imgCos: PhotoView? = null
    private var lottieView: LottieAnimationView? = null
    private var isLottieHidden = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_zoom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        loadImage()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun setupViews(view: View) {
        imgCos = view.findViewById(R.id.imgCos)
        lottieView = view.findViewById(R.id.lottie_animation)

        view.findViewById<ImageView>(R.id.btn_back_dialog).setOnClickListener {
            dismiss()
        }

        setupLottie()
        setupPhotoViewEvents()
    }

    private fun setupLottie() {
        lottieView?.apply {
            setAnimation(R.raw.zoom)
            playAnimation()
            visibility = View.VISIBLE
        }
    }

    private fun setupPhotoViewEvents() {
        imgCos?.setOnScaleChangeListener { _, _, _ ->
            hideLottie()
        }

        imgCos?.setOnViewTapListener { _, _, _ ->
            hideLottie()
        }
    }

    private fun hideLottie() {
        if (!isLottieHidden) {
            lottieView?.apply {
                visibility = View.GONE
                pauseAnimation()
            }
            isLottieHidden = true
        }
    }

    private fun loadImage() {
        val imageUrl = arguments?.getString(ARG_IMAGE_URL)
        imgCos?.loadImageFromNetwork(imageUrl ?: "")
    }

    companion object {
        private const val ARG_IMAGE_URL = "imageUrl"

        fun newInstance(imageUrl: String) = DialogZoom().apply {
            arguments = Bundle().apply {
                putString(ARG_IMAGE_URL, imageUrl)
            }
        }
    }
}