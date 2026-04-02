package com.example.slotmachinegame.imageviewscrolling

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.slotmachinegame.databinding.ImageViewScrollingBinding

class ImageViewScrolling @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var iEventEnd: IEventEnd? = null
    var lastResult = 0
    var oldValue = 0
    var value: Int

    val ANIMATION_DURATION = 150
    private var binding: ImageViewScrollingBinding =
        ImageViewScrollingBinding.inflate(LayoutInflater.from(context), this, true)

    private var listSlotData: List<SlotData> = mutableListOf()

    init {
        value = binding.nextImage.tag.toString().toInt()
    }

    fun setListSlotData(list: List<SlotData>) {
        listSlotData = list
    }

    fun setEventEnd(event: IEventEnd) {
        this.iEventEnd = event
    }

    fun setValueRandom(image: Int, numRotate: Int) {
        binding.currentImage.animate()
            .translationY((-height.toFloat()))
            .setDuration(ANIMATION_DURATION.toLong()).start()

        binding.nextImage.translationY = binding.nextImage.height.toFloat()

        binding.nextImage.animate().translationY(0f).setDuration(ANIMATION_DURATION.toLong())
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    setImage(binding.nextImage, oldValue % listSlotData.size)
                    setImage(binding.currentImage, lastResult)
                    binding.currentImage.translationY = 0f
                    if (oldValue != numRotate) {
                        setValueRandom(image, numRotate)
                        oldValue++
                    } else {
                        lastResult = 0
                        oldValue = 0
                        setImage(binding.nextImage, image)
                        setImage(binding.currentImage, image)
                        iEventEnd?.eventEnd(image % listSlotData.size, numRotate)
                    }
                }

                override fun onAnimationRepeat(animation: Animator) {
                }

                override fun onAnimationStart(animation: Animator) {
                }
            })
    }

    private fun setImage(currentImage: ImageView, i: Int) {
        currentImage.setImageResource(listSlotData[i].resDrawable)
        currentImage.tag = i
        lastResult = i
    }
}

