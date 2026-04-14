package com.hadat.aiyoga.sequence

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hadat.aiyoga.databinding.DialogTimeBinding
import com.hadat.aiyoga.databinding.FragmentSequencesBinding
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.utils.CloudinaryUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class SequencesFragment : BaseFragment<FragmentSequencesBinding, SequencesViewModel>(), OnStartDragListener {

    private val recommendAdapter by lazy { RecommendPoseAdapter() }
    private val args by navArgs<SequencesFragmentArgs>()
    private val poseAdapter by lazy {
        PoseSequenceAdapter(this) { item, position -> showTimePickerDialog(item, position) }
    }
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var selectedImageUri: Uri? = null
    private val defaultImageUrl = "https://plus.unsplash.com/premium_photo-1676815865390-8e3a9336f64b?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8MXx8eW9nYSUyMGJhY2tncm91bmR8ZW58MHx8MHx8fDA%3D"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivSequenceBackground.setImageURI(it)
            binding.layoutUploadHint.visibility = View.GONE
        }
    }

    override fun initView() {
        binding.rcvPeakOptions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = poseAdapter
        }
        binding.rcvRecommendations.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendAdapter
        }

        Glide.with(this).load(defaultImageUrl).into(binding.ivSequenceBackground)

        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return poseAdapter.onItemMove(vh.bindingAdapterPosition, target.bindingAdapterPosition)
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}
            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) vh?.itemView?.alpha = 0.8f
            }
            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                vh.itemView.alpha = 1.0f
                viewModel.updateList(poseAdapter.dataList)
            }
        }
        itemTouchHelper = ItemTouchHelper(callback).apply { attachToRecyclerView(binding.rcvPeakOptions) }

        viewModel.recommendationList.observe(viewLifecycleOwner) { recommendAdapter.setList(it) }

        viewModel.saveStatus.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                showToast("Sequence created successfully!")
                popBackStack()
            } else {
                showToast("Failed to save sequence")
                binding.btnCreate.isEnabled = true
            }
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.cardSelectImage.singleClick { pickImageLauncher.launch("image/*") }

        recommendAdapter.setOnClickItemRecyclerView { pose, _ ->
            val newPose = SequenceModel(
                id = pose.id.toString(),
                name = pose.name,
                category = pose.category,
                duration = "01:00",
                photoUrl = pose.photo_url
            )
            val currentList = poseAdapter.dataList.toMutableList().apply { add(newPose) }
            poseAdapter.setList(currentList)
            viewModel.updateList(currentList)
            binding.rcvPeakOptions.smoothScrollToPosition(currentList.size - 1)
        }

        binding.btnCreate.singleClick { handleCreateFlow() }
    }

    private fun handleCreateFlow() {
        val name = binding.edtSequenceName.text.toString().trim()
        if (name.isEmpty()) {
            showToast("Please enter sequence name")
            return
        }

        binding.btnCreate.isEnabled = false
        val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
        val level = when (binding.cgLevel.checkedChipId) {
            com.hadat.aiyoga.R.id.chip_beginner -> 1
            com.hadat.aiyoga.R.id.chip_intermediate -> 2
            com.hadat.aiyoga.R.id.chip_advanced -> 3
            else -> 1
        }

        selectedImageUri?.let { uri ->
            CloudinaryUtils.uploadImage(requireContext(), uri,
                onSuccess = { url -> viewModel.saveSequence(name, level, url, userId) },
                onError = {
                    showToast(it)
                    binding.btnCreate.isEnabled = true
                }
            )
        } ?: viewModel.saveSequence(name, level, defaultImageUrl, userId)
    }

    override fun initData() {
        viewModel.fetchAllPoses()
        args.selectedPosesList?.let { array ->
            val sequenceData = array.map { pose ->
                SequenceModel(id = pose.id.toString(), name = pose.name, category = pose.category, duration = "01:00", photoUrl = pose.photo_url)
            }
            poseAdapter.setList(sequenceData)
            viewModel.updateList(sequenceData)
        }
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    private fun showTimePickerDialog(item: SequenceModel, position: Int) {
        val dialog = Dialog(requireContext())
        val bindingDialog = DialogTimeBinding.inflate(layoutInflater)
        dialog.apply {
            setContentView(bindingDialog.root)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }

        bindingDialog.apply {
            npMinutes.minValue = 1
            npMinutes.maxValue = 9
            npSeconds.minValue = 0
            npSeconds.maxValue = 59

            val parts = item.duration.split(":")
            if (parts.size == 2) {
                npMinutes.value = parts[0].toInt()
                npSeconds.value = parts[1].toInt()
            }

            ivClose.setOnClickListener { dialog.dismiss() }
            btnConfirm.setOnClickListener {
                val newDuration = String.format("%02d:%02d", npMinutes.value, npSeconds.value)
                viewModel.updateDuration(position, newDuration)
                poseAdapter.notifyItemChanged(position)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}