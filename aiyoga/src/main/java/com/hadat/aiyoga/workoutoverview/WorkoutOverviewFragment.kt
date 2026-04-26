package com.hadat.aiyoga.workoutoverview

import android.app.DatePickerDialog
import android.graphics.Color
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
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

    private var fromCalendar: Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    private var toCalendar: Calendar = Calendar.getInstance()

    override fun initView() {
        binding.viewPagerCharts.apply {
            adapter = chartAdapter
            offscreenPageLimit = 3
        }
        binding.dotsIndicator.attachTo(binding.viewPagerCharts)
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

        binding.tvFromDate.singleClick {
            pickDate(fromCalendar) { calendar ->
                fromCalendar = calendar
                renderDateTexts()
                fetchData()
            }
        }

        binding.tvToDate.singleClick {
            pickDate(toCalendar) { calendar ->
                toCalendar = calendar
                renderDateTexts()
                fetchData()
            }
        }
    }

    override fun initData() {
        renderDateTexts()
        fetchData()

        viewModel.sequences.observe(viewLifecycleOwner) { listResults ->
            toggleChartsVisibility(!listResults.isNullOrEmpty())
            if (listResults.isNullOrEmpty()) {
                resetUI()
                return@observe
            }
            processWorkoutData(listResults)
        }
    }

    private fun fetchData() {
        val userId = AppPreferences.getUserId(requireContext()) ?: return

        val start = (fromCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis

        val end = (toCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis

        viewModel.fetchRange(userId, start, end)
    }
    private fun toggleChartsVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
        binding.viewPagerCharts.visibility = visibility
        binding.dotsIndicator.visibility = visibility
    }
    private fun processWorkoutData(list: List<WorkoutResultModel>) {

        val weight = viewModel.userWeight.value ?: 60f
        val metMap = viewModel.metDataMap.value ?: emptyMap()
        val dailyGoal = viewModel.dailyGoalCalories.value ?: 300

        val diffMillis = toCalendar.timeInMillis - fromCalendar.timeInMillis
        val daysSelected = (diffMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val totalRangeGoal = dailyGoal.toFloat() * daysSelected

        var totalCalo = 0f
        var totalSec = 0L

        list.forEach { item ->
            totalSec += item.durationInSeconds

            val met = metMap[item.poseId] ?: 3.0
            totalCalo += HealthCalculatorUtils.calculateWorkoutCaloriesByMet(
                met,
                weight,
                item.durationInSeconds
            )
        }

        val totalMin = totalSec / 60f

        val totalAcc = list.map {
            HealthCalculatorUtils.calculateAccuracy(
                expectedTimeSec = it.durationInSeconds,
                wrongCount = it.errorCount
            )
        }.let { if (it.isEmpty()) 100f else it.average().toFloat() }

        updateProgressUI(totalCalo, totalMin, totalAcc, totalRangeGoal)

        val dailyGrouped = list.groupBy {
            modelDateFormat.format(Date(it.workoutTimestamp))
        }

        val labels = mutableListOf<String>()
        val caloEntries = mutableListOf<BarEntry>()
        val accTrendEntries = mutableListOf<Entry>()

        dailyGrouped.values.forEachIndexed { index, sessions ->

            val x = index.toFloat()
            labels.add(dateFormatLabel.format(Date(sessions.first().workoutTimestamp)))

            var dayCalo = 0f

            sessions.forEach { item ->
                val met = metMap[item.poseId] ?: 3.0
                dayCalo += HealthCalculatorUtils.calculateWorkoutCaloriesByMet(
                    met,
                    weight,
                    item.durationInSeconds
                )
            }

            val dayAcc = sessions.map {
                HealthCalculatorUtils.calculateAccuracy(
                    it.durationInSeconds,
                    it.errorCount
                )
            }.let { if (it.isEmpty()) 100f else it.average().toFloat() }

            caloEntries.add(BarEntry(x, dayCalo))
            accTrendEntries.add(Entry(x, dayAcc))
        }

        val pieEntries = list.groupBy { it.poseId }.map {
            PieEntry(it.value.size.toFloat(), "Pose ${it.key}")
        }

        chartAdapter.updateData(
            ChartDataModel("Calories (kcal)", caloEntries, Color.parseColor("#FF6A00")),
            ChartDataModel("Accuracy Trend (%)", accTrendEntries, Color.parseColor("#00C853")),
            pieEntries,
            labels
        )
    }

    private fun updateProgressUI(calo: Float, minutes: Float, acc: Float, targetCalo: Float) {
        binding.progressCalories.apply {
            progressMax = targetCalo
            setProgressWithAnimation(calo, 1200)
        }
        binding.tvCaloriesValue.text = "%.0f".format(calo)

        val days = (targetCalo / (viewModel.dailyGoalCalories.value ?: 300)).toInt().coerceAtLeast(1)
        binding.progressTime.apply {
            progressMax = 30f * days
            setProgressWithAnimation(minutes, 1200)
        }
        binding.tvTimeValue.text = "${minutes.toInt()} min"

        binding.progressAccuracy.apply {
            progressBarColor = when {
                acc >= 85 -> Color.parseColor("#00C853")
                acc >= 60 -> Color.parseColor("#FFD600")
                else -> Color.parseColor("#FF6A00")
            }
            setProgressWithAnimation(acc, 1200)
        }
        binding.tvAccuracyValue.text = "${acc.toInt()}%"
    }

    private fun renderDateTexts() {
        binding.tvFromDate.text = dateFormatDisplay.format(fromCalendar.time)
        binding.tvToDate.text = dateFormatDisplay.format(toCalendar.time)
    }

    private fun pickDate(initial: Calendar, onPicked: (Calendar) -> Unit) {
        DatePickerDialog(requireContext(), { _, y, m, d ->
            onPicked(Calendar.getInstance().apply { set(y, m, d) })
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun parseDurationSeconds(duration: String): Int {
        val parts = duration.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }

    private fun resetUI() {
        val defaultGoal = (viewModel.dailyGoalCalories.value ?: 300).toFloat() * 7
        updateProgressUI(0f, 0f, 0f, defaultGoal)
        chartAdapter.updateData(
            ChartDataModel("Calories", emptyList(), Color.GRAY),
            ChartDataModel("Accuracy", emptyList(), Color.GRAY),
            emptyList(),
            emptyList()
        )
    }
}