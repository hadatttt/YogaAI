package com.hadat.aiyoga.detailsequence

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentSequenceDetailBinding
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class DetailSequenceFragment : BaseFragment<FragmentSequenceDetailBinding, DetailSequenceViewModel>() {

    private val args by navArgs<DetailSequenceFragmentArgs>()

    private val poseAdapter by lazy { PoseDetailAdapter() }

    override fun initView() {
        binding.rcvPoseList.adapter = poseAdapter
    }

    override fun initData() {
        args.detailSequence?.let { sequence ->
            viewModel.setDetailData(sequence)
            viewModel.refreshSequence(sequence.id)
        }
    }

    override fun initListener() {
        binding.ivEdit.singleClick {
            val sequence = viewModel.sequenceData.value ?: return@singleClick

            val bundle = Bundle().apply {
                putParcelable("detail_sequence", sequence)
                putBoolean("isEdit", true)
                putParcelableArray("selected_poses_list", null)
            }
            navigate(R.id.sequencesFragment, bundle,isPop = true)
        }
        binding.ivBack.singleClick {
            popBackStack()
        }

        binding.ivVisibility.singleClick {
            val current = viewModel.sequenceData.value ?: return@singleClick
            if (viewModel.visibilityUpdating.value == true) return@singleClick
            viewModel.setIsPublic(!current.isPublic)
        }
        onBackPressed {
            popBackStack()
        }

        viewModel.sequenceData.observe(viewLifecycleOwner) { sequence ->
            binding.apply {
                tvDetailName.text = sequence.title
                tvDetailDuration.text = formatDurationToMin(sequence.totalDuration)
                tvDetailPosesCount.text = getString(R.string.format_poses_count, sequence.poses.size)

                ivSequenceCover.loadImageFromNetwork(sequence.coverImageUrl)

                poseAdapter.setList(sequence.poses)

                ivVisibility.setImageResource(
                    if (sequence.isPublic) R.drawable.ic_public else R.drawable.ic_lock
                )
                tvLikeCount.text = sequence.likeCount.toString()
                tvViewCount.text = sequence.viewCount.toString()

                val showStats = sequence.isPublic
                ivLike.visibility = if (showStats) View.VISIBLE else View.GONE
                tvLikeCount.visibility = if (showStats) View.VISIBLE else View.GONE
                ivView.visibility = if (showStats) View.VISIBLE else View.GONE
                tvViewCount.visibility = if (showStats) View.VISIBLE else View.GONE
            }
        }

        binding.btnStartWorkout.singleClick {
            val sequence = viewModel.sequenceData.value ?: return@singleClick
            val bundle = Bundle().apply {
                putParcelable("detail_sequence", sequence)
            }
            navigate(R.id.multiModeYogaFragment, bundle, isPop = true)
        }
    }

    private fun formatDurationToMin(duration: String): String {
        return try {
            val parts = duration.split(":")
            val minutes = if (parts.isNotEmpty()) {
                parts[0].toIntOrNull() ?: 0
            } else 0
            getString(R.string.format_minutes, minutes)
        } catch (_: Exception) {
            getString(R.string.format_minutes, 0)
        }
    }
}