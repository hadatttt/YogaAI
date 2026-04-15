package com.hadat.aiyoga.workoutoverview

import android.app.DatePickerDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.hadat.aiyoga.databinding.FragmentWorkoutOverviewBinding
import com.hadat.aiyoga.result.WorkoutHistoryAdapter
import com.hadat.aiyoga.service.AppPreferences
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkoutOverviewFragment : BaseFragment<FragmentWorkoutOverviewBinding, WorkoutOverviewViewModel>() {

    private val historyAdapter by lazy { WorkoutHistoryAdapter { } }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var fromCalendar: Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    private var toCalendar: Calendar = Calendar.getInstance()

    override fun initView() {

    }

    override fun initData() {
        renderDateTexts()
        fetch()
        viewModel.results.observe(viewLifecycleOwner) { historyAdapter.setList(it) }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }
        binding.tvFromDate.singleClick { pickDate(fromCalendar) { fromCalendar = it; renderDateTexts(); fetch() } }
        binding.tvToDate.singleClick { pickDate(toCalendar) { toCalendar = it; renderDateTexts(); fetch() } }
    }

    private fun fetch() {
        val userId = AppPreferences.getUserId(requireContext()) ?: return
        val fromMillis = fromCalendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val toMillis = toCalendar.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
        viewModel.fetchRange(userId, fromMillis, toMillis)
    }

    private fun renderDateTexts() {
        binding.tvFromDate.text = dateFormat.format(fromCalendar.time)
        binding.tvToDate.text = dateFormat.format(toCalendar.time)
    }

    private fun pickDate(initial: Calendar, onPicked: (Calendar) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                onPicked(Calendar.getInstance().apply { set(year, month, dayOfMonth) })
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}

