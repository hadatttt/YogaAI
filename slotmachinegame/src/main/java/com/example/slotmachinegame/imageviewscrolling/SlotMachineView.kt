package com.example.slotmachinegame.imageviewscrolling

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.slotmachinegame.databinding.SlotMachineViewBinding
import kotlin.random.Random

@SuppressLint("ViewConstructor")
class SlotMachineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    interface SpinningMachineCallBack {
        fun startSpinning()
        fun endSpinning(value1: Int, value2: Int, value3: Int)
    }

    private var spinningMachineCallBack: SpinningMachineCallBack? = null
    fun setSpinningMachineCallBack(callback: SpinningMachineCallBack) {
        this.spinningMachineCallBack = callback
    }

    private val binding = SlotMachineViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val slot1 = binding.slot1
    private val slot2 = binding.slot2
    private val slot3 = binding.slot3

    private var onImageClickListener: (() -> Unit)? = null

    fun setOnImageClickListener(listener: () -> Unit) {
        onImageClickListener = listener
    }

    private var listBonusSymBol: List<SlotData> = emptyList()

    fun setBonusSymbols(list: List<SlotData>) {
        listBonusSymBol = list
        setDataForSlot()
    }

    var countDown = 0
    val eventEnd = object : IEventEnd {
        override fun eventEnd(result: Int, count: Int) {
            if (countDown < 2) {
                countDown++
            } else {
                countDown = 0
                spinningMachineCallBack?.endSpinning(slot1.lastResult, slot2.lastResult, slot3.lastResult)
            }
        }
    }

    fun changeState(isRunning: Boolean){
        binding.btnSlotMachine.isVisible = !isRunning
        binding.btnSlotMachine2.isVisible = isRunning
    }

    init {
        binding.btnSlotMachine.setOnClickListener {
            if (countDown != 0) return@setOnClickListener
            onImageClickListener?.invoke()
        }
        setDataForSlot()
        setEventEnd()
    }

    fun spin(){
        binding.btnSlotMachine.callOnClick()
    }

    private fun setEventEnd() {
        slot1.setEventEnd(eventEnd)
        slot2.setEventEnd(eventEnd)
        slot3.setEventEnd(eventEnd)
    }

    fun setControlledRandomResult(chanceSameResultPercent: Int = 10, minRotate: Int, maxRotate: Int) {
        val sameResult = Random.nextInt(30) <= chanceSameResultPercent
        val range = maxRotate - minRotate + 1
        if (sameResult) {
            val value = Random.nextInt(6)
            slot1.setValueRandom(value, Random.nextInt(range) + minRotate)
            slot2.setValueRandom(value, Random.nextInt(range) + minRotate)
            slot3.setValueRandom(value, Random.nextInt(range) + minRotate)
        } else {
            slot1.setValueRandom(Random.nextInt(6), Random.nextInt(range) + minRotate)
            slot2.setValueRandom(Random.nextInt(6), Random.nextInt(range) + minRotate)
            slot3.setValueRandom(Random.nextInt(6), Random.nextInt(range) + minRotate)
        }
    }

    fun setValueRandom(minRotate: Int, maxRotate: Int) {
        spinningMachineCallBack?.startSpinning()
        setControlledRandomResult(12, minRotate, maxRotate)
    }

    private fun setDataForSlot() {
        slot1.setListSlotData(listBonusSymBol)
        slot2.setListSlotData(listBonusSymBol)
        slot3.setListSlotData(listBonusSymBol)
    }


}