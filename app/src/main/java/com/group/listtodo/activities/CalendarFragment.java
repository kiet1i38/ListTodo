package com.group.listtodo.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.group.listtodo.R;
import com.group.listtodo.adapters.TaskAdapter;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import com.group.listtodo.utils.SessionManager; // <--- Đừng quên Import cái này

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
    private String userId; // <--- Biến lưu ID người dùng hiện tại

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(getContext());

        // 1. Lấy User ID từ Session (Đã đăng nhập)
        SessionManager session = new SessionManager(getContext());
        userId = session.getUserId();

        calendarView = view.findViewById(R.id.calendar_view);
        recyclerView = view.findViewById(R.id.rv_calendar_tasks);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                // Xử lý khi bấm vào task (Sửa)
            }

            @Override
            public void onTaskCheck(Task task) {
                updateTaskStatus(task);
            }
        });

        recyclerView.setAdapter(adapter);

        // Mặc định chọn hôm nay
        updateSelectedDateRange(System.currentTimeMillis());
        loadTasksForDate();

        // Sự kiện chọn ngày
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth);
            updateSelectedDateRange(c.getTimeInMillis());
            loadTasksForDate();
        });
    }

    private void updateSelectedDateRange(long timeInMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeInMillis);

        // Reset về đầu ngày (00:00:00)
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        selectedDateStart = c.getTimeInMillis();

        // Reset về cuối ngày (23:59:59)
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        selectedDateEnd = c.getTimeInMillis();
    }

    private void loadTasksForDate() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // SỬA LỖI TẠI ĐÂY: Truyền userId vào hàm query
            List<Task> allTasks = db.taskDao().getAllTasks(userId);
            List<Task> tasksForDate = new ArrayList<>();

            for (Task t : allTasks) {
                // Kiểm tra xem task có nằm trong khoảng ngày đã chọn không
                if (t.dueDate >= selectedDateStart && t.dueDate <= selectedDateEnd) {
                    tasksForDate.add(t);
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.setData(tasksForDate);
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