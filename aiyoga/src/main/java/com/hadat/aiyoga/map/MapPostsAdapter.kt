package com.hadat.aiyoga.map

import android.view.LayoutInflater
import android.view.ViewGroup
import com.hadat.aiyoga.data.firestore.model.MapPostModel
import com.hadat.aiyoga.databinding.ItemMapPostBinding
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder
import hoang.dqm.codebase.utils.singleClick
import java.text.SimpleDateFormat
import java.util.Locale

class MapPostsAdapter(
    private val onItemClick: (MapPostModel) -> Unit
) : BaseRecyclerViewAdapter<MapPostModel, ItemMapPostBinding>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ItemMapPostBinding> {
        val binding = ItemMapPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(binding)
    }

    override fun bindData(binding: ItemMapPostBinding, item: MapPostModel, position: Int) {
        binding.root.singleClick { onItemClick(item) }

        binding.tvName.text = item.userName.ifBlank { item.userId }
        binding.tvDescription.text = item.description

        val date = item.createdAt?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
        } ?: ""
        binding.tvTime.text = date

        binding.imgAvatar.loadImageFromNetwork(item.userAvatar)
        val bg = item.imageUrls.firstOrNull().orEmpty()
        if (bg.isNotBlank()) binding.imgBackground.loadImageFromNetwork(bg)
    }
}

