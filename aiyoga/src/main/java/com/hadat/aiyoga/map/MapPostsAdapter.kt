package com.hadat.aiyoga.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.firestore.model.MapPostModel
import com.hadat.aiyoga.databinding.ItemMapPostBinding
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.base.adapter.BaseViewHolder
import hoang.dqm.codebase.utils.singleClick
import java.text.SimpleDateFormat
import java.util.Locale

class MapPostsAdapter(
    private val showDelete: Boolean = false,
    private val onDeleteClick: ((MapPostModel) -> Unit)? = null,
    private val onItemClick: (MapPostModel) -> Unit
) : BaseRecyclerViewAdapter<MapPostModel, ItemMapPostBinding>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ItemMapPostBinding> {
        val binding = ItemMapPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(binding)
    }


    override fun bindData(binding: ItemMapPostBinding, item: MapPostModel, position: Int) {
        binding.root.singleClick { onItemClick(item) }

        binding.tvName.text = item.userName.ifBlank { "User ${item.userId.takeLast(4)}" }
        binding.tvDescription.text = item.description
        binding.btnDelete.visibility = if (showDelete) View.VISIBLE else View.GONE
        binding.btnDelete.singleClick { onDeleteClick?.invoke(item) }

        val date = item.createdAt?.let {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: "Vừa xong"
        binding.tvTime.text = date
        binding.imgAvatar.loadImageFromNetwork(item.userAvatar)
        val bgImage = item.imageUrls

        if (bgImage.isNotBlank()) {
            binding.imgBackground.loadImageFromNetwork(bgImage)
        } else {
            binding.imgBackground.setImageResource(R.drawable.ic_practice)
        }
    }
}

