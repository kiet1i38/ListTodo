package com.group.listtodo.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import com.group.listtodo.utils.SessionManager; // <--- Import quan trọng

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsActivity extends AppCompatActivity {

    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvPending, tvDone, tvOverdue, tvTodaySummary;
    private AppDatabase db;
    private String userId; // <--- Khai báo biến userId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        db = AppDatabase.getInstance(this);

        // 1. Lấy User ID từ Session
        SessionManager session = new SessionManager(this);
        userId = session.getUserId();

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
            // 2. Truyền userId vào câu lệnh database (Sửa lỗi tại đây)
            List<Task> allTasks = db.taskDao().getAllTasks(userId);

            int pending = 0;
            int done = 0;
            int overdue = 0;
            int todayDone = 0;
            int todayTotal = 0;
            long now = System.currentTimeMillis();

            // Tính toán số liệu
            for (Task t : allTasks) {
                if (t.isCompleted) {
                    done++;
                    if (isToday(t.dueDate)) todayDone++;
                } else {
                    if (t.dueDate < now) overdue++;
                    else pending++;
                }
                if (isToday(t.dueDate)) todayTotal++;
            }

            // Tính toán dữ liệu cho biểu đồ cột (7 ngày qua)
            List<BarEntry> barEntries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -i);
                int tasksThatDay = countTasksForDate(allTasks, cal);
                barEntries.add(new BarEntry(6 - i, tasksThatDay));
                labels.add(i == 0 ? "H.Nay" : "-" + i + "d");
            }

            int finalPending = pending;
            int finalDone = done;
            int finalOverdue = overdue;
            int finalTodayDone = todayDone;
            int finalTodayTotal = todayTotal;

            runOnUiThread(() -> {
                // Update Text
                tvPending.setText(String.valueOf(finalPending));
                tvDone.setText(String.valueOf(finalDone));
                tvOverdue.setText(String.valueOf(finalOverdue));
                tvTodaySummary.setText(finalTodayDone + "/" + finalTodayTotal + " công việc");

                // Vẽ Chart
                setupPieChart(finalDone, finalPending + finalOverdue);
                setupBarChart(barEntries, labels);
            });
        });
    }

    private boolean isToday(long timeInMillis) {
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(timeInMillis);
        Calendar now = Calendar.getInstance();
        return t.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                t.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
    }

    private int countTasksForDate(List<Task> tasks, Calendar date) {
        int count = 0;
        for (Task t : tasks) {
            Calendar tCal = Calendar.getInstance();
            tCal.setTimeInMillis(t.dueDate);
            if (tCal.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                    tCal.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) &&
                    t.isCompleted) {
                count++;
            }
        }
        return count;
    }

    private void setupPieChart(int done, int notDone) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(done, "Xong"));
        entries.add(new PieEntry(notDone, "Chưa"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#34C759"), Color.parseColor("#E0E0E0"));
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setHoleRadius(60f);
        pieChart.setCenterText(done + "");
        pieChart.setCenterTextSize(20f);
        pieChart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupBarChart(List<BarEntry> entries, List<String> labels) {
        BarDataSet dataSet = new BarDataSet(entries, "Đã hoàn thành");
        dataSet.setColor(Color.parseColor("#246BFD"));
        dataSet.setDrawValues(true);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        barChart.invalidate();
    }
}