package com.group.listtodo.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsActivity extends AppCompatActivity {

    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvPending, tvDone, tvOverdue, tvTodaySummary;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        db = AppDatabase.getInstance(this);

        initViews();
        loadStatistics();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void initViews() {
        pieChart = findViewById(R.id.pieChart_today);
        barChart = findViewById(R.id.barChart_weekly);
        tvPending = findViewById(R.id.tv_stat_pending);
        tvDone = findViewById(R.id.tv_stat_done);
        tvOverdue = findViewById(R.id.tv_stat_overdue);
        tvTodaySummary = findViewById(R.id.tv_today_summary);
    }

    private void loadStatistics() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> allTasks = db.taskDao().getAllTasks();

            int pending = 0;
            int done = 0;
            int overdue = 0;
            int todayTotal = 0;
            int todayDone = 0;
            long now = System.currentTimeMillis();

            // Tính toán số liệu
            for (Task t : allTasks) {
                if (t.isCompleted) {
                    done++;
                    // Check if today (Demo logic: simply check if completed)
                    todayDone++;
                } else {
                    if (t.dueDate < now) overdue++;
                    else pending++;
                }
                todayTotal++; // Demo logic
            }

            int finalPending = pending;
            int finalDone = done;
            int finalOverdue = overdue;
            int finalTodayDone = todayDone;
            int finalTodayTotal = todayTotal;

            runOnUiThread(() -> {
                // Update số liệu text
                tvPending.setText(String.valueOf(finalPending));
                tvDone.setText(String.valueOf(finalDone));
                tvOverdue.setText(String.valueOf(finalOverdue));
                tvTodaySummary.setText("Đã xong " + finalTodayDone + "/" + finalTodayTotal + " việc");

                // Vẽ biểu đồ
                setupPieChart(finalTodayDone, finalTodayTotal - finalTodayDone);
                setupBarChart(); // Demo dummy data for bar chart
            });
        });
    }

    private void setupPieChart(int done, int remaining) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(done, ""));
        entries.add(new PieEntry(remaining, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#246BFD"), Color.parseColor("#E0E0E0")); // Xanh & Xám
        dataSet.setDrawValues(false); // Không hiện số trên biểu đồ cho đẹp

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setHoleRadius(70f); // Lỗ tròn ở giữa
        pieChart.setCenterText(String.valueOf((int)((float)done/(done+remaining)*100)) + "%");
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        pieChart.invalidate(); // Refresh
    }

    private void setupBarChart() {
        // Demo data giả lập cho biểu đồ cột (7 ngày)
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1, 2));
        entries.add(new BarEntry(2, 4));
        entries.add(new BarEntry(3, 1));
        entries.add(new BarEntry(4, 5));
        entries.add(new BarEntry(5, 3));
        entries.add(new BarEntry(6, 2));
        entries.add(new BarEntry(7, 4));

        BarDataSet dataSet = new BarDataSet(entries, "Tasks Done");
        dataSet.setColor(Color.parseColor("#246BFD"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.invalidate();
    }
}