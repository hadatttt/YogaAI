package com.hadat.aiyoga.detailsequence

import android.os.Bundle
import androidx.navigation.fragment.navArgs
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentSequenceDetailBinding
import com.hadat.aiyoga.utils.loadImageFromNetwork
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class DetailSequenceFragment : BaseFragment<FragmentSequenceDetailBinding, DetailSequenceViewModel>() {

    private val args by navArgs<DetailSequenceFragmentArgs>()

    private val poseAdapter by lazy { PoseDetailAdapter() }

    override fun initView() {
        binding.rcvPoseList.adapter = poseAdapter
    }

    override fun initData() {
        args.detailSequence?.let {
            viewModel.setDetailData(it)
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
            navigate(R.id.sequencesFragment, bundle)
        }
        binding.ivBack.singleClick {
            popBackStack()
        }

        viewModel.sequenceData.observe(viewLifecycleOwner) { sequence ->
            binding.apply {
                tvDetailName.text = sequence.title
                tvDetailDuration.text = formatDurationToMin(sequence.totalDuration)
                tvDetailPosesCount.text = "${sequence.poses.size} Poses"

                ivSequenceCover.loadImageFromNetwork(sequence.coverImageUrl)

                poseAdapter.setList(sequence.poses)

                ivVisibility.setImageResource(
                    if (sequence.isPublic) R.drawable.ic_public else R.drawable.ic_lock
                )
            }
        }

        binding.btnStartWorkout.singleClick {
        }
    }

    private fun formatDurationToMin(duration: String): String {
        return try {
            val parts = duration.split(":")
            if (parts.isNotEmpty()) {
                val minutes = parts[0].toIntOrNull() ?: 0
                "$minutes min"
            } else {
                "0 min"
            }
        } catch (_: Exception) {
            "0 min"
        }
    }
}