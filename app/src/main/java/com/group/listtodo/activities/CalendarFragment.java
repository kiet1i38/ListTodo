package com.group.listtodo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.group.listtodo.R;
import com.group.listtodo.adapters.TaskAdapter;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import com.group.listtodo.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private AppDatabase db;
    private long selectedDateStart, selectedDateEnd;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(getContext());

        SessionManager session = new SessionManager(getContext());
        userId = session.getUserId();

        calendarView = view.findViewById(R.id.calendar_view);
        recyclerView = view.findViewById(R.id.rv_calendar_tasks);

        setupRecyclerView();

        // Mặc định chọn hôm nay
        updateSelectedDateRange(System.currentTimeMillis());
        loadTasksForDate();

        // Sự kiện chọn ngày trên lịch
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth);
            updateSelectedDateRange(c.getTimeInMillis());
            loadTasksForDate();
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // --- SỬA LỖI TẠI ĐÂY: CUNG CẤP ĐỦ 2 THAM SỐ ---
        adapter = new TaskAdapter(
                // Tham số 1: Xử lý Task (Sửa/Check)
                new TaskAdapter.OnTaskClickListener() {
                    @Override
                    public void onTaskClick(Task task) {
                        Intent intent = new Intent(getContext(), EditTaskActivity.class);
                        intent.putExtra("task", task);
                        startActivity(intent);
                    }

                    @Override
                    public void onTaskCheck(Task task) {
                        updateTaskStatus(task);
                    }
                },
                // Tham số 2: Xử lý Header (Ở Lịch không dùng header đóng mở nên để trống)
                new TaskAdapter.OnHeaderClickListener() {
                    @Override
                    public void onHeaderClick(String headerTitle) {
                        // Không làm gì cả
                    }
                }
        );
        // -----------------------------------------------

        recyclerView.setAdapter(adapter);
    }

    private void updateSelectedDateRange(long timeInMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeInMillis);

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        selectedDateStart = c.getTimeInMillis();

        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        selectedDateEnd = c.getTimeInMillis();
    }

    private void loadTasksForDate() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> allTasks = db.taskDao().getAllTasks(userId);

            List<Task> tasksForDate = new ArrayList<>();
            for (Task t : allTasks) {
                if (t.dueDate >= selectedDateStart && t.dueDate <= selectedDateEnd) {
                    tasksForDate.add(t);
                }
            }

            // Chuyển đổi sang Wrapper cho Adapter mới
            List<TaskAdapter.TaskItemWrapper> displayList = new ArrayList<>();
            for (Task t : tasksForDate) {
                displayList.add(new TaskAdapter.TaskItemWrapper(t));
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.setData(displayList);
                });
            }
        });
    }

    private void updateTaskStatus(Task task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            db.taskDao().updateTask(task);
            loadTasksForDate();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasksForDate();
    }
}