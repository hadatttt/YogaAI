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
    private val onSelectionChanged: ((selectedUri: String) -> Unit)? = null
) : BaseRecyclerViewAdapter<String, ItemImageThumbBinding>() {

    private var selectedItem: String? = null

    fun setSelected(uri: String) {
        if (selectedItem == uri) return
        val oldIndex = dataList.indexOf(selectedItem)
        val newIndex = dataList.indexOf(uri)
        selectedItem = uri
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }

    fun clearSelection() {
        val oldIndex = dataList.indexOf(selectedItem)
        selectedItem = null
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ItemImageThumbBinding> {
        val binding = ItemImageThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(binding)
    }

    override fun bindData(binding: ItemImageThumbBinding, item: String, position: Int) {
        if (item.isBlank()) return
        Glide.with(binding.ivThumb)
            .load(if (item.startsWith("content://") || item.startsWith("file://")) Uri.parse(item) else item)
            .into(binding.ivThumb)

        val isSelected = selectedItem == item
        binding.viewSelectedStroke.isVisible = selectable && isSelected
        binding.ivSelectedCheck.isVisible = selectable && isSelected

        binding.root.singleClick {
            if (selectable) {
                if (selectedItem == item) return@singleClick
                setSelected(item)
                onSelectionChanged?.invoke(item)
            }
        }
    }
}