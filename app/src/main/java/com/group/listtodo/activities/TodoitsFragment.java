package com.group.listtodo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.group.listtodo.R;
import com.group.listtodo.adapters.TaskAdapter;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TodoitsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private AppDatabase db;
    private LinearLayout layoutEmpty;
    private ImageButton btnMoreOptions;

    // Trạng thái bộ lọc mặc định
    private boolean showCompleted = false; // Mặc định ẩn việc đã xong cho gọn
    private boolean showFuture = true;
    private boolean showOverdue = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_todoits, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(getContext());

        // Ánh xạ View
        recyclerView = view.findViewById(R.id.recycler_tasks);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);

        setupRecyclerView();
        setupSwipeToDelete(); // Tính năng Tuần 5: Vuốt để xóa

        // Xử lý sự kiện click nút FAB
        fab.setOnClickListener(v -> {
            AddNewTaskSheet bottomSheet = new AddNewTaskSheet(() -> loadTasks());
            bottomSheet.show(getParentFragmentManager(), "AddTask");
        });

        // Mở Menu 3 chấm Filter
        btnMoreOptions.setOnClickListener(v -> showFilterMenu());

        // Load dữ liệu lần đầu
        loadTasks();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                // Mở màn hình sửa (EditTaskActivity)
                Intent intent = new Intent(getContext(), EditTaskActivity.class);
                intent.putExtra("task", task); // Truyền object task sang
                startActivity(intent);
            }

            @Override
            public void onTaskCheck(Task task) {
                updateTaskStatus(task);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    // TÍNH NĂNG NOVELTY/ADVANCED GUI: Vuốt sang trái để xóa
    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // Không hỗ trợ kéo thả di chuyển vị trí
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // Lấy task cần xóa từ Adapter (Cần đảm bảo Adapter có hàm getTaskList)
                // Nếu chưa có hàm getTaskList trong adapter, em xem lưu ý bên dưới
                Task taskToDelete = adapter.getTaskList().get(position);

                deleteTask(taskToDelete);
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void deleteTask(Task task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            db.taskDao().deleteTask(task);

            // Cập nhật UI
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã xóa: " + task.title, Toast.LENGTH_SHORT).show();
                    loadTasks(); // Reload lại list
                });
            }
        });
    }

    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(getContext(), btnMoreOptions);
        popup.getMenuInflater().inflate(R.menu.menu_todoits_filter, popup.getMenu());

        // Đánh dấu (Check) vào các mục đang được chọn
        popup.getMenu().findItem(R.id.action_show_completed).setChecked(showCompleted);
        popup.getMenu().findItem(R.id.action_show_future).setChecked(showFuture);
        popup.getMenu().findItem(R.id.action_show_overdue).setChecked(showOverdue);

        // Xử lý khi chọn item trong menu
        popup.setOnMenuItemClickListener(item -> {
            item.setChecked(!item.isChecked());

            int id = item.getItemId();
            if (id == R.id.action_show_completed) {
                showCompleted = item.isChecked();
            } else if (id == R.id.action_show_future) {
                showFuture = item.isChecked();
            } else if (id == R.id.action_show_overdue) {
                showOverdue = item.isChecked();
            }

            loadTasks();
            return true;
        });

        popup.show();
    }

    private void loadTasks() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> allTasks = db.taskDao().getAllTasks();
            List<Task> filteredList = new ArrayList<>();

            long now = System.currentTimeMillis();

            for (Task t : allTasks) {
                boolean isTaskOverdue = t.dueDate < now && !t.isCompleted;
                boolean isTaskFuture = t.dueDate >= now;
                boolean isTaskCompleted = t.isCompleted;

                if (isTaskCompleted && !showCompleted) continue;
                if (isTaskFuture && !showFuture && !isTaskCompleted) continue;
                if (isTaskOverdue && !showOverdue) continue;

                filteredList.add(t);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (filteredList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.setData(filteredList);
                    }
                });
            }
        });
    }

    private void updateTaskStatus(Task task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            db.taskDao().updateTask(task);
            loadTasks();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks(); // Load lại khi quay lại từ màn hình Edit hoặc Add
    }
}