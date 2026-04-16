package com.hadat.aiyoga.workoutoverview

import android.app.DatePickerDialog
import android.graphics.Color
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
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

    // Format ngày lưu trong Firestore/Model của bạn
    private val modelDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    // Format rút gọn để hiển thị dưới trục X biểu đồ
    private val dateFormatLabel = SimpleDateFormat("dd/MM", Locale.getDefault())
    // Format hiển thị đầy đủ trên thanh chọn ngày
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

    override fun initData() {
        renderDateTexts()
        fetch()

        viewModel.sequences.observe(viewLifecycleOwner) { listResults ->
            if (listResults.isNullOrEmpty()) {
                resetUI()
                return@observe
            }

            val parsedDurations = listResults.map { sequence ->
                parseDurationSeconds(sequence.totalDuration)
            }

            val totalSec = parsedDurations.sum().toLong()
            val totalCalo = totalSec * 0.15f
            val totalMin = totalSec / 60f
            val totalAcc = listResults.map { sequence ->
                if (sequence.viewCount > 0) {
                    (sequence.likeCount.toFloat() / sequence.viewCount.toFloat() * 100f).coerceIn(10f, 100f)
                } else {
                    100f
                }
            }.average().toFloat()

            updateProgressBars(totalCalo, totalMin, totalAcc)

            val sortedList = listResults.sortedBy { it.createdAt?.time ?: 0L }
            val dailyGrouped = sortedList.groupBy {
                val date = it.createdAt ?: Date()
                modelDateFormat.format(date)
            }

            val labels = mutableListOf<String>()
            val caloEntries = mutableListOf<BarEntry>()
            val accTrendEntries = mutableListOf<Entry>()

            dailyGrouped.values.forEachIndexed { index, listPerDay ->
                val x = index.toFloat()
                val firstDate = listPerDay.firstOrNull()?.createdAt ?: Date()
                labels.add(dateFormatLabel.format(firstDate))

                val dayCalo = listPerDay.sumOf { parseDurationSeconds(it.totalDuration).toDouble() }.toFloat() * 0.15f
                val dayAcc = listPerDay.map {
                    if (it.viewCount > 0) {
                        (it.likeCount.toFloat() / it.viewCount.toFloat() * 100f).coerceIn(10f, 100f)
                    } else {
                        100f
                    }
                }.average().toFloat()

                caloEntries.add(BarEntry(x, dayCalo))
                accTrendEntries.add(Entry(x, dayAcc))
            }

            val levelGroups = listResults.groupBy { "Level ${it.level}" }
            val pieEntries = levelGroups.map { PieEntry(it.value.size.toFloat(), it.key) }

            val barModel = ChartDataModel("Calories (kcal)", caloEntries, Color.parseColor("#FF6A00"))
            val lineModel = ChartDataModel("Accuracy Trend (%)", accTrendEntries, Color.parseColor("#4A90E2"))

            chartAdapter.updateData(barModel, lineModel, pieEntries, labels)
        }
    }

    private fun updateProgressBars(calo: Float, minutes: Float, acc: Float) {
        binding.progressCalories.apply {
            progressMax = 1000f // Có thể chỉnh mục tiêu tùy ý
            setProgressWithAnimation(calo, 1000)
        }
        binding.tvCaloriesValue.text = "%.0f".format(calo)

        binding.progressTime.apply {
            progressMax = 300f
            setProgressWithAnimation(minutes, 1000)
        }
        binding.tvTimeValue.text = "${minutes.toInt()} min"

        binding.progressAccuracy.setProgressWithAnimation(acc, 1000)
        binding.tvAccuracyValue.text = "${acc.toInt()}%"
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

        binding.tvFromDate.singleClick {
            pickDate(fromCalendar) { fromCalendar = it; renderDateTexts(); fetch() }
        }

        binding.tvToDate.singleClick {
            pickDate(toCalendar) { toCalendar = it; renderDateTexts(); fetch() }
        }
    }

    private fun fetch() {
        val userId = AppPreferences.getUserId(requireContext()) ?: return
        val start = (fromCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis

        val end = (toCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis

        viewModel.fetchRange(userId, start, end)
    }

    private fun renderDateTexts() {
        binding.tvFromDate.text = dateFormatDisplay.format(fromCalendar.time)
        binding.tvToDate.text = dateFormatDisplay.format(toCalendar.time)
    }

    private fun resetUI() {
        updateProgressBars(0f, 0f, 0f)

        // Tạo dữ liệu trống thay vì truyền null
        val emptyBar = ChartDataModel("Calories", emptyList(), Color.GRAY)
        val emptyLine = ChartDataModel("Accuracy", emptyList(), Color.GRAY)

        chartAdapter.updateData(emptyBar, emptyLine, emptyList(), emptyList())
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
}
