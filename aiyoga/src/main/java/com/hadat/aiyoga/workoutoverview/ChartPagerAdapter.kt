package com.hadat.aiyoga.workoutoverview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.hadat.aiyoga.databinding.ItemChartPageBinding

data class ChartDataModel(val title: String, val entries: List<Entry>, val color: Int)
data class TopPoseDataModel(val title: String, val entries: List<BarEntry>, val labels: List<String>)

class ChartPagerAdapter : RecyclerView.Adapter<ChartPagerAdapter.ChartViewHolder>() {

    private var barDataModel: ChartDataModel? = null
    private var lineDataModel: ChartDataModel? = null
    private var topPoseModel: TopPoseDataModel? = null
    private var dateLabels: List<String> = listOf()

    fun updateData(bar: ChartDataModel, line: ChartDataModel, topPose: TopPoseDataModel, labels: List<String>) {
        this.barDataModel = bar
        this.lineDataModel = line
        this.topPoseModel = topPose
        this.dateLabels = labels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        val binding = ItemChartPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        when (position) {
            0 -> barDataModel?.let { holder.bindBarChart(it, dateLabels) }
            1 -> lineDataModel?.let { holder.bindLineChart(it, dateLabels) }
            2 -> topPoseModel?.let { holder.bindHorizontalBarChart(it) }
        }
    }

    override fun getItemCount(): Int = 3

    class ChartViewHolder(private val binding: ItemChartPageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindBarChart(model: ChartDataModel, labels: List<String>) {
            showChart(binding.barChart)
            val dataSet = BarDataSet(model.entries as List<BarEntry>, model.title).apply {
                color = model.color
                setDrawValues(false)
            }
            binding.barChart.apply {
                data = BarData(dataSet).apply { barWidth = 0.5f }
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                description.isEnabled = false
                animateY(800)
                invalidate()
            }
        }

        fun bindLineChart(model: ChartDataModel, labels: List<String>) {
            showChart(binding.lineChart)
            val dataSet = LineDataSet(model.entries, model.title).apply {
                color = model.color
                setCircleColor(model.color)
                lineWidth = 2f
                circleRadius = 4f
                setDrawFilled(true)
                fillAlpha = 50
                fillColor = model.color
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            binding.lineChart.apply {
                data = LineData(dataSet)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                description.isEnabled = false
                animateX(800)
                invalidate()
            }
        }

        fun bindHorizontalBarChart(model: TopPoseDataModel) {
            showChart(binding.horizontalBarChart)
            val dataSet = BarDataSet(model.entries, model.title).apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextSize = 10f
            }
            binding.horizontalBarChart.apply {
                data = BarData(dataSet).apply { barWidth = 0.6f }
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(model.labels)
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                }
                axisRight.isEnabled = false
                description.isEnabled = false
                animateY(1000)
                invalidate()
            }
        }

        private fun showChart(view: View) {
            binding.barChart.visibility = View.GONE
            binding.lineChart.visibility = View.GONE
            binding.horizontalBarChart.visibility = View.GONE
            view.visibility = View.VISIBLE
        }
    }
}