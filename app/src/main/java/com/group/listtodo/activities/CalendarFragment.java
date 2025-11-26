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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(getContext());

        calendarView = view.findViewById(R.id.calendar_view);
        recyclerView = view.findViewById(R.id.rv_calendar_tasks);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // --- SỬA LỖI TẠI ĐÂY: Thêm Listener để xử lý tick chọn ---
        adapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                // Xử lý khi bấm vào task (Sửa) nếu cần
            }

            @Override
            public void onTaskCheck(Task task) {
                // Khi tick vào checkbox -> Cập nhật DB -> Load lại list
                updateTaskStatus(task);
            }
        });
        // ----------------------------------------------------------

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
            List<Task> allTasks = db.taskDao().getAllTasks();
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

    // Hàm cập nhật trạng thái hoàn thành vào Database
    private void updateTaskStatus(Task task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            db.taskDao().updateTask(task);
            // Sau khi update xong thì load lại list của ngày hôm đó để cập nhật giao diện (gạch ngang chữ)
            loadTasksForDate();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasksForDate();
    }
}