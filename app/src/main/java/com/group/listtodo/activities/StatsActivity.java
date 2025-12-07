package com.group.listtodo.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import com.group.listtodo.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsActivity extends AppCompatActivity {

    private PieChart pieChartToday, pieChartMain;
    private TextView tvPending, tvDone, tvOverdue, tvTodaySummary;
    private TextView tvFilterLabel, tvMainChartDesc;
    private TextView tvCountTotal, tvCountPending, tvCountDone, tvCountOverdue, tvDayName;
    private LinearLayout btnFilterTime;

    private AppDatabase db;
    private String userId;
    private int filterType = 1; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getUserId();

        initViews();
        setupEvents();

        loadStatistics();
    }

    private void initViews() {
        pieChartToday = findViewById(R.id.pieChart_today);
        pieChartMain = findViewById(R.id.pieChart_main);

        tvPending = findViewById(R.id.tv_stat_pending);
        tvDone = findViewById(R.id.tv_stat_done);
        tvOverdue = findViewById(R.id.tv_stat_overdue);
        tvTodaySummary = findViewById(R.id.tv_today_summary);

        tvFilterLabel = findViewById(R.id.tv_filter_label);
        tvMainChartDesc = findViewById(R.id.tv_main_chart_desc);
        btnFilterTime = findViewById(R.id.btn_filter_time);

        tvCountTotal = findViewById(R.id.tv_count_total);
        tvCountPending = findViewById(R.id.tv_count_pending);
        tvCountDone = findViewById(R.id.tv_count_done);
        tvCountOverdue = findViewById(R.id.tv_count_overdue);
        tvDayName = findViewById(R.id.tv_day_name);

        tvDayName.setText(new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date()));
    }

    private void setupEvents() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnFilterTime.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, btnFilterTime);
            popup.getMenu().add(0, 1, 0, "Tuần");
            popup.getMenu().add(0, 2, 0, "Tháng");
            popup.getMenu().add(0, 3, 0, "Năm");

            popup.setOnMenuItemClickListener(item -> {
                filterType = item.getItemId();
                tvFilterLabel.setText(item.getTitle());
                loadStatistics(); 
                return true;
            });
            popup.show();
        });
    }

    private void loadStatistics() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> allTasks = db.taskDao().getAllTasks(userId);
            long now = System.currentTimeMillis();

            // --- 1. THỐNG KÊ HÔM NAY & TỔNG QUAN ---
            int todayDone = 0;
            int todayTotal = 0;
            int totalPending = 0;
            int totalDone = 0;
            int totalOverdue = 0;

            for (Task t : allTasks) {
                if (isToday(t.dueDate)) {
                    todayTotal++;
                    if (t.isCompleted) todayDone++;
                }

                if (t.isCompleted) totalDone++;
                else if (t.dueDate < now) totalOverdue++;
                else totalPending++;
            }

            // --- 2. THỐNG KÊ THEO FILTER (Tuần/Tháng/Năm) ---
            int filterTotal = 0;
            int filterDone = 0;

            Calendar calStart = Calendar.getInstance();
            if (filterType == 1) calStart.add(Calendar.WEEK_OF_YEAR, -1);
            else if (filterType == 2) calStart.add(Calendar.MONTH, -1);
            else calStart.add(Calendar.YEAR, -1);

            long startTime = calStart.getTimeInMillis();

            for (Task t : allTasks) {
                if (t.dueDate >= startTime) {
                    filterTotal++;
                    if (t.isCompleted) filterDone++;
                }
            }

            // --- 3. THỐNG KÊ CHI TIẾT TUẦN NÀY (Bottom) ---
            // Logic: Đếm số lượng task trong 7 ngày gần nhất
            int weekTotal = 0;
            int weekPending = 0;
            int weekDone = 0;
            int weekOverdue = 0;

            Calendar weekCal = Calendar.getInstance();
            weekCal.add(Calendar.DAY_OF_YEAR, -7);
            long weekStart = weekCal.getTimeInMillis();

            for (Task t : allTasks) {
                if (t.dueDate >= weekStart) {
                    weekTotal++;
                    if (t.isCompleted) weekDone++;
                    else if (t.dueDate < now) weekOverdue++;
                    else weekPending++;
                }
            }

            // Final variables for UI Thread
            int finalTodayDone = todayDone;
            int finalTodayTotal = todayTotal;
            int finalTotalPending = totalPending;
            int finalTotalDone = totalDone;
            int finalTotalOverdue = totalOverdue;

            int finalFilterTotal = filterTotal;
            int finalFilterDone = filterDone;

            int fWeekTotal = weekTotal;
            int fWeekPending = weekPending;
            int fWeekDone = weekDone;
            int fWeekOverdue = weekOverdue;

            runOnUiThread(() -> {
                // Update Top Cards
                tvTodaySummary.setText("Có tổng cộng " + finalTodayTotal + " mục trong\nchương trình hôm nay");
                tvPending.setText(String.valueOf(finalTotalPending) + " >");
                tvDone.setText(String.valueOf(finalTotalDone) + " >");
                tvOverdue.setText(String.valueOf(finalTotalOverdue) + " >");

                // Chart Hôm nay
                setupPieChart(pieChartToday, finalTodayDone, finalTodayTotal - finalTodayDone, finalTodayDone, 14f);

                // Chart Lớn (Giữa)
                setupPieChart(pieChartMain, finalFilterDone, finalFilterTotal - finalFilterDone, finalFilterTotal, 40f);
                String timeText = (filterType == 1) ? "tuần" : (filterType == 2 ? "tháng" : "năm");
                tvMainChartDesc.setText("Có " + finalFilterTotal + " mục trong chương trình " + timeText + " này");

                // Update Bottom Stats
                tvCountTotal.setText(String.valueOf(fWeekTotal));
                tvCountPending.setText(String.valueOf(fWeekPending));
                tvCountDone.setText(String.valueOf(fWeekDone));
                tvCountOverdue.setText(String.valueOf(fWeekOverdue));
            });
        });
    }

    private void setupPieChart(PieChart chart, int val1, int val2, int centerVal, float centerTextSize) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(val1, "")); // Phần màu xanh (Đã xong)
        entries.add(new PieEntry(val2, "")); // Phần màu xám (Chưa xong)

        PieDataSet dataSet = new PieDataSet(entries, "");
        // Màu: Xanh đậm (#246BFD) và Xám nhạt (#F0F2F5)
        dataSet.setColors(Color.parseColor("#246BFD"), Color.parseColor("#F0F2F5"));
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(0f); // Khoảng cách giữa các miếng

        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setHoleRadius(85f); // Lỗ tròn to (để tạo thành vòng Ring mỏng)
        chart.setTransparentCircleRadius(0f);

        chart.setCenterText(String.valueOf(centerVal));
        chart.setCenterTextSize(centerTextSize);
        chart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        chart.setCenterTextColor(Color.parseColor("#246BFD"));

        chart.setTouchEnabled(false); // Không cho xoay/bấm
        chart.invalidate();
    }

    private boolean isToday(long timeInMillis) {
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(timeInMillis);
        Calendar now = Calendar.getInstance();
        return t.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                t.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
    }
}
