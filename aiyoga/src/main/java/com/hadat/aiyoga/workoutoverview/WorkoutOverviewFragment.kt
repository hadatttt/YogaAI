package com.hadat.aiyoga.workoutoverview

import android.app.DatePickerDialog
import android.graphics.Color
import android.view.View
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.hadat.aiyoga.R
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils
import com.hadat.aiyoga.databinding.FragmentWorkoutOverviewBinding
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.yoga_single.WorkoutResultModel
import com.hadat.aiyoga.utils.yogautils.HealthCalculatorUtils
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.text.SimpleDateFormat
import java.util.*

class WorkoutOverviewFragment : BaseFragment<FragmentWorkoutOverviewBinding, WorkoutOverviewViewModel>() {

    private val chartAdapter by lazy { ChartPagerAdapter() }
    private val modelDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateFormatLabel = SimpleDateFormat("dd/MM", Locale.getDefault())
    private val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var fromCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    private var toCalendar = Calendar.getInstance()

    override fun initView() {
        binding.viewPagerCharts.apply {
            adapter = chartAdapter
            offscreenPageLimit = 3
        }
        binding.dotsIndicator.attachTo(binding.viewPagerCharts)
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.tvFromDate.singleClick { pickDate(fromCalendar) { fromCalendar = it; renderDateTexts(); fetchData() } }
        binding.tvToDate.singleClick { pickDate(toCalendar) { toCalendar = it; renderDateTexts(); fetchData() } }
    }

    override fun initData() {
        renderDateTexts()
        fetchData()
        viewModel.sequences.observe(viewLifecycleOwner) { list ->
            toggleChartsVisibility(!list.isNullOrEmpty())
            if (list.isNullOrEmpty()) resetUI() else processWorkoutData(list)
        }
    }

    private fun fetchData() {
        val userId = AppPreferences.getUserId(requireContext()) ?: return
        val start = fromCalendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
        val end = toCalendar.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis
        viewModel.fetchRange(userId, start, end)
    }

    private fun toggleChartsVisibility(isVisible: Boolean) {
        val v = if (isVisible) View.VISIBLE else View.GONE
        binding.viewPagerCharts.visibility = v
        binding.dotsIndicator.visibility = v
    }

    private fun processWorkoutData(list: List<WorkoutResultModel>) {
        val weight = viewModel.userWeight.value ?: 60f
        val metMap = viewModel.metDataMap.value ?: emptyMap()
        val dailyGoal = (viewModel.dailyGoalCalories.value ?: 300).toFloat()
        val daysSelected = ((toCalendar.timeInMillis - fromCalendar.timeInMillis) / 86400000).toInt().coerceAtLeast(1)
        val totalRangeGoal = dailyGoal * daysSelected

        var totalCalo = 0f
        var totalSec = 0L

        list.forEach {
            totalSec += it.durationInSeconds
            totalCalo += HealthCalculatorUtils.calculateWorkoutCaloriesByMet(metMap[it.poseId] ?: 3.0, weight, it.durationInSeconds)
        }

        val aiList = list.filter { it.isAiMode }
        val totalAcc = aiList.map { HealthCalculatorUtils.calculateAccuracy(it.durationInSeconds, it.errorCount) }
            .let { if (it.isEmpty()) 0f else it.average().toFloat() }

        updateProgressUI(totalCalo, totalSec / 60f, totalAcc, totalRangeGoal)

        val dailyGrouped = list.groupBy { modelDateFormat.format(Date(it.workoutTimestamp)) }
        val labels = mutableListOf<String>()
        val caloEntries = mutableListOf<BarEntry>()
        val accTrendEntries = mutableListOf<Entry>()

        dailyGrouped.values.forEachIndexed { index, sessions ->
            val x = index.toFloat()
            labels.add(dateFormatLabel.format(Date(sessions.first().workoutTimestamp)))
            val dCalo = sessions.sumOf { HealthCalculatorUtils.calculateWorkoutCaloriesByMet(metMap[it.poseId] ?: 3.0, weight, it.durationInSeconds).toDouble() }.toFloat()
            val aiSessions = sessions.filter { it.isAiMode }
            val dAcc = if (aiSessions.isEmpty()) {
                0f
            } else {
                aiSessions.map { HealthCalculatorUtils.calculateAccuracy(it.durationInSeconds, it.errorCount) }.average().toFloat()
            }
            caloEntries.add(BarEntry(x, dCalo))
            accTrendEntries.add(Entry(x, dAcc))
        }

        val top5Poses = list.groupBy { it.poseId }
            .map { entry ->
                val poseId = entry.key
                val poseInfo = YogaDataUtils.getPoseById(poseId)
                val localizedName = poseInfo?.name ?: getString(R.string.unknown_pose)
                val totalMinutes = entry.value.sumOf { it.durationInSeconds } / 60f
                localizedName to totalMinutes
            }
            .sortedByDescending { it.second }
            .take(5)
            .reversed()

        val topLabels = top5Poses.map { it.first }
        val topEntries = top5Poses.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second)
        }

        val topPoseModel = TopPoseDataModel(
            getString(R.string.chart_title_top_poses),
            topEntries,
            topLabels
        )
        chartAdapter.updateData(
            ChartDataModel(getString(R.string.chart_title_calories), caloEntries, Color.parseColor("#FF6A00")),
            ChartDataModel(getString(R.string.chart_title_accuracy), accTrendEntries, Color.parseColor("#00C853")),
            topPoseModel,
            labels
        )
    }

    private fun updateProgressUI(calo: Float, minutes: Float, acc: Float, targetCalo: Float) {
        binding.progressCalories.apply { progressMax = targetCalo; setProgressWithAnimation(calo, 1200) }
        binding.tvCaloriesValue.text = "%.0f".format(calo)
        binding.progressTime.apply { progressMax = 30f * (targetCalo / 300).toInt().coerceAtLeast(1); setProgressWithAnimation(minutes, 1200) }
        binding.tvTimeValue.text = "${minutes.toInt()} ${getString(R.string.unit_min)}"
        binding.progressAccuracy.apply {
            progressBarColor = if (acc >= 85) Color.parseColor("#00C853") else if (acc >= 60) Color.parseColor("#FFD600") else Color.parseColor("#FF6A00")
            setProgressWithAnimation(acc, 1200)
        }
        binding.tvAccuracyValue.text = "${acc.toInt()}%"
    }

    private fun renderDateTexts() {
        binding.tvFromDate.text = dateFormatDisplay.format(fromCalendar.time)
        binding.tvToDate.text = dateFormatDisplay.format(toCalendar.time)
    }

    private fun pickDate(initial: Calendar, onPicked: (Calendar) -> Unit) {
        DatePickerDialog(requireContext(), { _, y, m, d -> onPicked(Calendar.getInstance().apply { set(y, m, d) }) },
            initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun resetUI() {
        val goal = (viewModel.dailyGoalCalories.value ?: 300).toFloat() * 7
        updateProgressUI(0f, 0f, 0f, goal)
        chartAdapter.updateData(
            ChartDataModel(getString(R.string.chart_title_calories), emptyList(), Color.GRAY),
            ChartDataModel(getString(R.string.chart_title_accuracy), emptyList(), Color.GRAY),
            TopPoseDataModel(getString(R.string.chart_title_top_poses), emptyList(), emptyList()),
            emptyList()
        )
    }
}
