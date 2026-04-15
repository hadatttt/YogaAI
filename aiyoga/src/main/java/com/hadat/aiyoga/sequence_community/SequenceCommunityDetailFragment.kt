package com.hadat.aiyoga.sequence_community

import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentSequenceCommunityDetailBinding
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.detailsequence.PoseDetailAdapter
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class SequenceCommunityDetailFragment :
    BaseFragment<FragmentSequenceCommunityDetailBinding, SequenceCommunityDetailViewModel>() {

    private val args by navArgs<SequenceCommunityDetailFragmentArgs>()
    private val poseAdapter by lazy { PoseDetailAdapter() }

    override fun initView() {
        binding.rcvPoseList.adapter = poseAdapter
    }

    override fun initData() {
        val sequence = args.detailSequence ?: return
        val userId = AppPreferences.getUserId(requireContext()) ?: ""
        viewModel.setDetailData(sequence, userId)
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

        binding.btnLikeAction.singleClick {
            val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
            viewModel.toggleLike(userId)
        }

        binding.btnCopyAction.singleClick {
            val userId = AppPreferences.getUserId(requireContext()) ?: "guest"
            viewModel.copyToMySequences(userId)
        }

        viewModel.sequenceData.observe(viewLifecycleOwner) { sequence ->
            binding.apply {
                tvDetailName.text = sequence.title
                tvDetailDuration.text = formatDurationToMin(sequence.totalDuration)
                tvDetailPosesCount.text = "${sequence.poses.size} Poses"
                ivSequenceCover.loadImageFromNetwork(sequence.coverImageUrl)
                poseAdapter.setList(sequence.poses)
            }
        }

        viewModel.likeCount.observe(viewLifecycleOwner) { binding.tvLikeCount.text = it.toString() }
        viewModel.viewCount.observe(viewLifecycleOwner) { binding.tvViewCount.text = it.toString() }

        viewModel.isLiked.observe(viewLifecycleOwner) { liked ->
            binding.btnLikeAction.setIconResource(
                if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            binding.ivLike.setImageResource(
                if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
        }

        viewModel.copyStatus.observe(viewLifecycleOwner) { ok ->
            if (ok == null) return@observe
            showToast(if (ok) "Copied!" else "Copy failed")
            viewModel.resetCopyStatus()
        }
    }

    private fun formatDurationToMin(duration: String): String {
        return try {
            val parts = duration.split(":")
            val minutes = parts.getOrNull(0)?.toIntOrNull() ?: 0
            "$minutes min"
        } catch (_: Exception) {
            "0 min"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

