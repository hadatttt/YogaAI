package com.hadat.aiyoga.workoutoverview

import android.app.DatePickerDialog
import android.graphics.Color
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentWorkoutOverviewBinding
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
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

    private fun processWorkoutData(list: List<WorkoutSequenceModel>) {
        // 1. Tính toán tổng số liệu (Sử dụng hệ số 0.15f của ResultFragment)
        val totalSec = list.sumOf { parseDurationSeconds(it.totalDuration).toLong() }
        val totalCalo = totalSec * 0.15f
        val totalMin = totalSec / 60f

        // Tính Accuracy trung bình: (likeCount / viewCount)
        val totalAcc = list.filter { it.viewCount > 0 }.map {
            (it.likeCount.toFloat() / it.viewCount.toFloat() * 100f).coerceIn(10f, 100f)
        }.let { if (it.isEmpty()) 100f else it.average().toFloat() }

        updateProgressUI(totalCalo, totalMin, totalAcc)

        // 2. Phân nhóm theo ngày để vẽ biểu đồ
        val dailyGrouped = list.filter { it.createdAt != null }
            .sortedBy { it.createdAt }
            .groupBy { modelDateFormat.format(it.createdAt!!) }

        val labels = mutableListOf<String>()
        val caloEntries = mutableListOf<BarEntry>()
        val accTrendEntries = mutableListOf<Entry>()

        dailyGrouped.values.forEachIndexed { index, sessions ->
            val x = index.toFloat()
            val date = sessions.first().createdAt ?: Date()
            labels.add(dateFormatLabel.format(date))

            val dayCalo = sessions.sumOf { parseDurationSeconds(it.totalDuration).toDouble() }.toFloat() * 0.15f
            val dayAcc = sessions.filter { it.viewCount > 0 }.map {
                (it.likeCount.toFloat() / it.viewCount.toFloat() * 100f).coerceIn(10f, 100f)
            }.let { if (it.isEmpty()) 100f else it.average().toFloat() }

            caloEntries.add(BarEntry(x, dayCalo))
            accTrendEntries.add(Entry(x, dayAcc))
        }

        // 3. Phân bổ theo Level bài tập cho Pie Chart
        val pieEntries = list.groupBy { it.level }.map {
            PieEntry(it.value.size.toFloat(), "Level ${it.key}")
        }

        // 4. Đổ dữ liệu vào Charts
        chartAdapter.updateData(
            ChartDataModel("Calories (kcal)", caloEntries, Color.parseColor("#FF6A00")),
            ChartDataModel("Accuracy Trend (%)", accTrendEntries, Color.parseColor("#00C853")),
            pieEntries,
            labels
        )
    }

    private fun updateProgressUI(calo: Float, minutes: Float, acc: Float) {
        binding.progressCalories.apply {
            progressMax = 1000f // Target tuần
            setProgressWithAnimation(calo, 1200)
        }
        binding.tvCaloriesValue.text = "%.0f".format(calo)

        binding.progressTime.apply {
            progressMax = 300f
            setProgressWithAnimation(minutes, 1200)
        }
        binding.tvTimeValue.text = "${minutes.toInt()} min"

        // Đổi màu Progress Accuracy giống ResultFragment
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
        updateProgressUI(0f, 0f, 0f)
        chartAdapter.updateData(
            ChartDataModel("Calories", emptyList(), Color.GRAY),
            ChartDataModel("Accuracy", emptyList(), Color.GRAY),
            emptyList(),
            emptyList()
        )
    }
}