package com.hadat.aiyoga.sequence_community

import android.os.Bundle
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentSequenceCommunityDetailBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.utils.view.loadImageFromNetwork
import com.hadat.aiyoga.detailsequence.PoseDetailAdapter
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
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
        binding.btnStartWorkout.singleClick {
            val sequence = viewModel.sequenceData.value ?: return@singleClick
            val bundle = Bundle().apply {
                putParcelable("detail_sequence", sequence)
            }
            navigate(R.id.multiModeYogaFragment, bundle, isPop = true)
        }
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
                tvDetailPosesCount.text = getString(R.string.format_poses_count, sequence.poses.size)
                ivSequenceCover.loadImageFromNetwork(sequence.coverImageUrl)
                poseAdapter.setList(sequence.poses)
            }
        }

        viewModel.likeCount.observe(viewLifecycleOwner) { binding.tvLikeCount.text = it.toString() }
        viewModel.viewCount.observe(viewLifecycleOwner) { binding.tvViewCount.text = it.toString() }

        viewModel.isLiked.observe(viewLifecycleOwner) { liked ->
            val heartIcon = if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            binding.btnLikeAction.setIconResource(heartIcon)
            binding.ivLike.setImageResource(heartIcon)
        }

        viewModel.copyStatus.observe(viewLifecycleOwner) { ok ->
            if (ok == null) return@observe
            showToast(
                if (ok) getString(R.string.copied_successfully)
                else getString(R.string.copy_failed)
            )
            viewModel.resetCopyStatus()
        }
    }

    private fun formatDurationToMin(duration: String): String {
        val minutes = try {
            val parts = duration.split(":")
            parts.getOrNull(0)?.toIntOrNull() ?: 0
        } catch (_: Exception) {
            0
        }
        return getString(R.string.format_minutes, minutes)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}