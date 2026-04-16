package com.hadat.aiyoga.workoutoverview

import android.app.DatePickerDialog
import android.graphics.Color
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.hadat.aiyoga.databinding.FragmentWorkoutOverviewBinding
import com.hadat.aiyoga.service.AppPreferences
import com.hadat.aiyoga.singleyoga.WorkoutResultModel
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

        // --- CHẾ ĐỘ TEST ---
        mockData() // Hãy comment dòng này và mở fetch() khi đã có data thật
        // fetch()

        viewModel.results.observe(viewLifecycleOwner) { listResults ->
            if (listResults.isNullOrEmpty()) {
                resetUI()
                return@observe
            }

            // --- 1. TỔNG HỢP CHỈ SỐ (Dùng cho 3 vòng tròn Progress) ---
            val totalSec = listResults.sumOf { it.durationInSeconds.toLong() }
            val totalErr = listResults.sumOf { it.errorCount.toLong() }

            val totalCalo = totalSec * 0.15f
            val totalMin = totalSec / 60f
            val totalAcc = (100f - (totalErr * 5f)).coerceIn(10f, 100f)

            updateProgressBars(totalCalo, totalMin, totalAcc)

            // --- 2. CHUẨN BỊ DỮ LIỆU CHO CÁC LOẠI BIỂU ĐỒ ---

            // Sắp xếp danh sách theo thời gian thực (parse từ String date)
            val sortedList = listResults.sortedBy {
                try { modelDateFormat.parse(it.date) } catch (e: Exception) { Date(it.workoutTimestamp) }
            }

            // Group theo ngày để mỗi ngày là 1 cột/điểm trên biểu đồ
            val dailyGrouped = sortedList.groupBy { it.date }

            val labels = mutableListOf<String>()
            val caloEntries = mutableListOf<BarEntry>()
            val accTrendEntries = mutableListOf<Entry>()

            dailyGrouped.values.forEachIndexed { index, listPerDay ->
                val x = index.toFloat()

                // Lấy label dd/MM từ chuỗi dd/MM/yyyy
                val dateObj = try { modelDateFormat.parse(listPerDay[0].date) } catch (e: Exception) { null }
                labels.add(dateObj?.let { dateFormatLabel.format(it) } ?: listPerDay[0].date)

                // Tính toán cho từng ngày (Dùng .toDouble() để tránh lỗi Ambiguity)
                val dayCalo = listPerDay.sumOf { (it.durationInSeconds * 0.15).toDouble() }.toFloat()
                val dayAcc = listPerDay.map { (100f - (it.errorCount * 5f)).coerceIn(10f, 100f) }.average().toFloat()

                caloEntries.add(BarEntry(x, dayCalo))
                accTrendEntries.add(Entry(x, dayAcc))
            }

            // Biểu đồ tròn: Tỷ lệ các tư thế Pose
            val poseGroups = listResults.groupBy { it.poseName }
            val pieEntries = poseGroups.map { PieEntry(it.value.size.toFloat(), it.key) }

            // Gửi dữ liệu vào Adapter
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

    private fun mockData() {
        val mockList = mutableListOf<WorkoutResultModel>()
        val random = Random()
        val poses = listOf("Warrior I", "Cobra", "Tree Pose", "Downward Dog")

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = modelDateFormat.format(cal.time)

            // Mỗi ngày tập ngẫu nhiên 1-3 bài tập
            repeat(random.nextInt(3) + 1) {
                mockList.add(WorkoutResultModel(
                    poseName = poses[random.nextInt(poses.size)],
                    date = dateStr,
                    durationInSeconds = random.nextInt(1200) + 300,
                    errorCount = random.nextInt(6),
                    workoutTimestamp = cal.timeInMillis
                ))
            }
        }
        viewModel.results.postValue(mockList)
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
}