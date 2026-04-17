package com.hadat.aiyoga.workoutoverview

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.hadat.aiyoga.databinding.ItemChartPageBinding

class ChartPagerAdapter : RecyclerView.Adapter<ChartPagerAdapter.ChartViewHolder>() {

    private var barDataModel: ChartDataModel? = null
    private var lineDataModel: ChartDataModel? = null
    private var pieEntries: List<PieEntry> = listOf()
    private var dateLabels: List<String> = listOf()

    fun updateData(bar: ChartDataModel, line: ChartDataModel, pie: List<PieEntry>, labels: List<String>) {
        this.barDataModel = bar
        this.lineDataModel = line
        this.pieEntries = pie
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
            2 -> holder.bindPieChart(pieEntries)
        }
    }

    override fun getItemCount(): Int = 3

    class ChartViewHolder(private val binding: ItemChartPageBinding) : RecyclerView.ViewHolder(binding.root) {

        // 1. Biểu đồ Cột (Calories)
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

        // 2. Biểu đồ Đường (Accuracy xu hướng)
        fun bindLineChart(model: ChartDataModel, labels: List<String>) {
            showChart(binding.lineChart)
            val dataSet = LineDataSet(model.entries as List<Entry>, model.title).apply {
                color = model.color
                setCircleColor(model.color)
                lineWidth = 2f
                circleRadius = 4f
                setDrawFilled(true)
                fillAlpha = 50
                fillColor = model.color
                mode = LineDataSet.Mode.CUBIC_BEZIER // Đường cong mượt
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

        // 3. Biểu đồ Tròn (Tỷ lệ Pose)
        fun bindPieChart(entries: List<PieEntry>) {
            showChart(binding.pieChart)
            val dataSet = PieDataSet(entries, "Poses Distribution").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextSize = 12f
                valueTextColor = Color.WHITE
            }
            binding.pieChart.apply {
                data = PieData(dataSet)
                description.isEnabled = false
                centerText = "Workout\nPoses"
                setCenterTextSize(14f)
                holeRadius = 45f
                animateXY(800, 800)
                invalidate()
            }
        }

        private fun showChart(view: View) {
            binding.barChart.visibility = View.GONE
            binding.lineChart.visibility = View.GONE
            binding.pieChart.visibility = View.GONE
            view.visibility = View.VISIBLE
        }
    }
}
data class ChartDataModel(
    val title: String,
    val entries: List<Entry>,
    val color: Int
)