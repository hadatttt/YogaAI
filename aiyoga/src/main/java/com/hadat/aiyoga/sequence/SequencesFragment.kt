package com.hadat.aiyoga.sequence

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.DialogDeleteBinding
import com.hadat.aiyoga.databinding.DialogTimeBinding
import com.hadat.aiyoga.databinding.FragmentSequencesBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.utils.view.CloudinaryUtils
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class SequencesFragment : BaseFragment<FragmentSequencesBinding, SequencesViewModel>(), OnStartDragListener {

    private val recommendAdapter by lazy { RecommendPoseAdapter() }
    private val args by navArgs<SequencesFragmentArgs>()
    private val poseAdapter by lazy {
        PoseSequenceAdapter(
            dragListener = this,
            onTimeClick = { item, position ->
                showTimePickerDialog(item, position)
            },
            onDeleteClick = { item, position ->
                showDeleteDialog(item, position)
            }
        )
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
        if (args.isEdit) {
            binding.tvTitle.text = "Edit Sequence"
            binding.btnCreate.text = "Update Sequence"
        } else {
            binding.tvTitle.text = "Sequence Settings"
            binding.btnCreate.text = "Create Sequence"
        }
        binding.rcvPeakOptions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = poseAdapter
        }
        binding.rcvRecommendations.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendAdapter
        }

        if (!args.isEdit) {
            binding.ivSequenceBackground.loadImageFromNetwork(defaultImageUrl)
        }

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
            if (isSuccess == null) return@observe
            if (isSuccess) {
                val message = if (args.isEdit)
                    getString(R.string.updated_successfully)
                else
                    getString(R.string.created_successfully)
                showToast(message)
                val sequence = viewModel.lastSavedSequence.value
                if (sequence != null) {
                    val bundle = Bundle().apply {
                        putParcelable("detail_sequence", sequence)
                    }
                    viewModel.resetSaveStatus()
                    navigate(R.id.detailSequenceFragment,bundle, isPop = true)
                } else {
                    popBackStack()
                }
            } else {
                showToast(getString(R.string.failed_to_save_sequence))
                binding.btnCreate.isEnabled = true
            }
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.cardSelectImage.singleClick { pickImageLauncher.launch("image/*") }

        recommendAdapter.setOnClickItemRecyclerView { pose, _ ->
            val newPose = SequenceModel(
                id = pose.id,
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
            showToast(getString(R.string.please_enter_sequence_name))
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

        val onProcessComplete: (String) -> Unit = { url ->
            if (args.isEdit && args.detailSequence != null) {
                viewModel.updateSequence(
                    id = args.detailSequence!!.id,
                    title = name,
                    level = level,
                    coverUrl = url,
                    userId = userId,
                    isPublic = args.detailSequence!!.isPublic
                )
            } else {
                viewModel.saveSequence(name, level, url, userId)
            }
        }

        selectedImageUri?.let { uri ->
            CloudinaryUtils.uploadImage(requireContext(), uri,
                onSuccess = { url -> onProcessComplete(url) },
                onError = {
                    showToast(it)
                    binding.btnCreate.isEnabled = true
                }
            )
        } ?: onProcessComplete(args.detailSequence?.coverImageUrl ?: defaultImageUrl)
    }

    override fun initData() {
        viewModel.fetchAllPoses(requireContext())
        val aiPoses = args.aiPosesList
        if (aiPoses != null) {
            binding.apply {
                if (selectedImageUri == null) {
                    ivSequenceBackground.loadImageFromNetwork(defaultImageUrl)
                }
            }
            val listFromAi = aiPoses.toList()
            poseAdapter.setList(listFromAi)
            viewModel.updateList(listFromAi)
            return
        }
        if (args.isEdit && args.detailSequence != null) {
            val data = args.detailSequence!!

            binding.apply {
                edtSequenceName.setText(data.title)
                when (data.level) {
                    1 -> cgLevel.check(R.id.chip_beginner)
                    2 -> cgLevel.check(R.id.chip_intermediate)
                    3 -> cgLevel.check(R.id.chip_advanced)
                }
                if (selectedImageUri == null) {
                    ivSequenceBackground.loadImageFromNetwork(data.coverImageUrl)
                }
            }
            poseAdapter.setList(data.poses)
            viewModel.updateList(data.poses)

        }
        else {
            args.selectedPosesList?.let { array ->
                val sequenceData = array.map { pose ->
                    SequenceModel(
                        id = pose.id,
                        name = pose.name,
                        category = pose.category,
                        duration = "01:00",
                        photoUrl = pose.photo_url
                    )
                }
                poseAdapter.setList(sequenceData)
                viewModel.updateList(sequenceData)
            }
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
            npMinutes.minValue = 0
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
    private fun showDeleteDialog(item: SequenceModel, position: Int) {
        val dialog = Dialog(requireContext())
        val bindingDialog = DialogDeleteBinding.inflate(layoutInflater)

        dialog.apply {
            setContentView(bindingDialog.root)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.85).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        bindingDialog.apply {
            ivClose.singleClick {
                dialog.dismiss()
            }

            btnConfirm.singleClick {
                val currentList = poseAdapter.dataList.toMutableList()

                if (position in currentList.indices) {
                    currentList.removeAt(position)

                    poseAdapter.setList(currentList)
                    viewModel.updateList(currentList)
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}