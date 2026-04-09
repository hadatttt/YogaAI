package com.hadat.aiyoga.detailyoga

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentDetailYogaBinding
import com.hadat.aiyoga.utils.ViewUtils
import com.hadat.aiyoga.utils.loadImageFromNetwork
import com.hadat.aiyoga.yogautils.YogaDataUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import kotlin.getValue

class DetailYogaFragment : BaseFragment<FragmentDetailYogaBinding, DetailYogaViewModel>() {

    private val categoryAdapter by lazy {
        CategoryDetailAdapter()
    }
    private val args by navArgs<DetailYogaFragmentArgs>()
    private val benefitAdapter by lazy { BenefitAdapter() }
    private val stepAdapter by lazy { StepAdapter() }
    private val contraAdapter by lazy { ContraindicationAdapter() }
    private val descriptionAdapter by lazy { DescriptionAdapter() }
    private var yogaDetail: YogaPoseDetailModel? = null

    override fun initView() {
         val menuDetail = listOf(
            CategoryDetailModel(0, "Description", R.drawable.ic_benefit),
             CategoryDetailModel(2, "Steps", R.drawable.ic_practice),
            CategoryDetailModel(1, "Benefits", R.drawable.ic_benefit),
            CategoryDetailModel(3, "Caution", R.drawable.ic_warning)
        )
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        binding.rvYogaContent.apply {
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }

        categoryAdapter.setList(menuDetail)
        val yogaPoseItem = args.yogaPoseItem
        binding.apply {
            tvPoseName.text = yogaPoseItem.name
            tvSanskritName.text = yogaPoseItem.name
            val levelText = when (yogaPoseItem.expertise_level) {
                1 -> "Beginner"
                2 -> "Intermediate"
                3 -> "Advanced"
                else -> "Unknown"
            }
            tvLevelValue.text = levelText
            imgYogaPose.loadImageFromNetwork(yogaPoseItem.photo_url)
        }
        YogaDataUtils.getRemoteYogaDetail(yogaPoseItem.id) { detail ->
            detail?.let {
                yogaDetail = it
                updateContentByCategoryId(0)
            }
        }
    }

    override fun initData() {

    }

    private fun updateContentByCategoryId(id: Int) {
        val detail = yogaDetail ?: return

        when (id) {
            0 -> {
                binding.rvYogaContent.adapter = descriptionAdapter
                descriptionAdapter.setList(listOf(detail.description))
            }
            1 -> {
                binding.rvYogaContent.adapter = benefitAdapter
                benefitAdapter.setList(detail.benefits)
            }
            2 -> {
                binding.rvYogaContent.adapter = stepAdapter
                stepAdapter.setList(detail.steps)
            }
            3 -> {
                binding.rvYogaContent.adapter = contraAdapter
                contraAdapter.setList(detail.contraindications)
            }
        }

        binding.rvYogaContent.scrollToPosition(0)
    }

    override fun initListener() {
        binding.ivBack.singleClick {
            popBackStack()
        }
        categoryAdapter.setOnClickItemRecyclerView { category, position ->
            categoryAdapter.setSelectedPosition(position)
            ViewUtils.scrollToCenter(binding.rvCategory, position)
            updateContentByCategoryId(category.id)
        }
        binding.layoutNext.singleClick {
            val bundle = Bundle().apply {
                putParcelable("yogaPoseItem", args.yogaPoseItem)
            }
            navigate(R.id.singleYogaFragment, bundle)
        }
    }
}