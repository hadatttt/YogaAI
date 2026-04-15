package com.hadat.aiyoga.result

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.hadat.aiyoga.databinding.ItemImageThumbBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder
import hoang.dqm.codebase.utils.singleClick

class CapturedImagesAdapter(
    private val selectable: Boolean = false,
    private val onSelectionChanged: ((selected: List<String>) -> Unit)? = null
) : BaseRecyclerViewAdapter<String, ItemImageThumbBinding>() {

    private var selectedItem: String? = null

    fun setSelected(list: List<String>) {
        selectedItem = list.firstOrNull()
        notifyDataSetChanged()
    }

    fun getSelected(): List<String> = selectedItem?.let(::listOf).orEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ItemImageThumbBinding> {
        val binding = ItemImageThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(binding)
    }

    override fun bindData(binding: ItemImageThumbBinding, item: String, position: Int) {
        Glide.with(binding.ivThumb).load(Uri.parse(item)).into(binding.ivThumb)

        val isSelected = selectedItem == item
        binding.viewSelectedStroke.isVisible = selectable && isSelected
        binding.ivSelectedCheck.isVisible = selectable && isSelected

        binding.root.singleClick {
            if (selectable) {
                if (selectedItem == item) return@singleClick
                val previous = selectedItem
                selectedItem = item
                previous?.let {
                    val oldIndex = dataList.indexOf(it)
                    if (oldIndex >= 0) notifyItemChanged(oldIndex)
                }
                notifyItemChanged(position)
                onSelectionChanged?.invoke(getSelected())
            }
        }
    }
}

