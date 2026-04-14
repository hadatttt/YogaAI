package com.hadat.aiyoga.sequence

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.hadat.aiyoga.databinding.ItemPoseSequenceBinding
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import java.util.Collections

class PoseSequenceAdapter(
    private val dragListener: OnStartDragListener,
    private val onTimeClick: (SequenceModel, Int) -> Unit
) : BaseRecyclerViewAdapter<SequenceModel, ItemPoseSequenceBinding>() {

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(dataList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(dataList, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun bindData(binding: ItemPoseSequenceBinding, item: SequenceModel, position: Int) {
        binding.apply {
            tvName.text = item.name
            tvCategory.text = item.category
            tvDuration.text = item.duration
            ivThump.loadImageFromNetwork(item.photoUrl)

            icDrag.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val holder = recyclerView.getChildViewHolder(binding.root)
                    holder?.let { dragListener.onStartDrag(it) }
                }
                false
            }

            tvDuration.setOnClickListener {
                val holder = recyclerView.getChildViewHolder(binding.root)
                onTimeClick(item, holder.bindingAdapterPosition)
            }
        }
    }
}

interface OnStartDragListener {
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
}