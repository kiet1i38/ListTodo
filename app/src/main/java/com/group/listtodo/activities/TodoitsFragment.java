package com.group.listtodo.activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.group.listtodo.R;
import com.group.listtodo.adapters.CategoryAdapter;
import com.group.listtodo.adapters.TaskAdapter;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import com.group.listtodo.utils.SessionManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TodoitsFragment extends Fragment {

    private RecyclerView rvTasks, rvCategories;
    private TaskAdapter taskAdapter;
    private CategoryAdapter categoryAdapter;
    private AppDatabase db;
    private LinearLayout layoutEmpty;
    private ImageButton btnMoreOptions;
    private String userId;

    // Danh sách danh mục mặc định
    private List<String> categoryList = new ArrayList<>(Arrays.asList("Tất Cả", "Công Việc", "Cá Nhân", "Học Tập", "+"));
    private String currentCategory = "Tất Cả";

    // Trạng thái đóng mở các nhóm (Mặc định mở)
    private boolean isOverdueExpanded = true;
    private boolean isFutureExpanded = true;
    private boolean isCompletedExpanded = true;

    // Biến lọc 3 chấm
    private boolean showCompleted = true;
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
        SessionManager session = new SessionManager(getContext());
        userId = session.getUserId();

        rvTasks = view.findViewById(R.id.recycler_tasks);
        rvCategories = view.findViewById(R.id.recycler_categories);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);

        // 1. Setup Danh mục
        setupCategoryRecycler();

        // 2. Setup Danh sách Task
        setupTaskRecycler();

        // 3. Setup Vuốt để xóa
        setupSwipeToDelete();

        fab.setOnClickListener(v -> {
            AddNewTaskSheet bottomSheet = new AddNewTaskSheet(() -> loadTasks());
            bottomSheet.show(getParentFragmentManager(), "AddTask");
        });

        btnMoreOptions.setOnClickListener(v -> showFilterMenu());

        loadTasks();
    }

    private void setupCategoryRecycler() {
        categoryAdapter = new CategoryAdapter(categoryList, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(String category) {
                currentCategory = category;
                loadTasks();
            }

            @Override
            public void onAddCategoryClick() {
                showAddCategoryDialog();
            }
        });
        rvCategories.setAdapter(categoryAdapter);
    }

    // Custom Dialog thêm danh mục (Đẹp hơn AlertDialog mặc định)
    private void showAddCategoryDialog() {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_category);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText edtName = dialog.findViewById(R.id.edt_category_name);
        ImageView btnClear = dialog.findViewById(R.id.btn_clear_text);
        TextView btnCancel = dialog.findViewById(R.id.btn_cancel);
        TextView btnConfirm = dialog.findViewById(R.id.btn_confirm);

        btnClear.setOnClickListener(v -> edtName.setText(""));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String newCat = edtName.getText().toString().trim();
            if (!newCat.isEmpty()) {
                categoryList.add(categoryList.size() - 1, newCat);
                categoryAdapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void setupTaskRecycler() {
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        taskAdapter = new TaskAdapter(
                // Listener cho Task
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
                // Listener cho Header (Đóng/Mở)
                new TaskAdapter.OnHeaderClickListener() {
                    @Override
                    public void onHeaderClick(String headerTitle) {
                        if (headerTitle.startsWith("Hết hạn")) {
                            isOverdueExpanded = !isOverdueExpanded;
                        } else if (headerTitle.startsWith("Việc cần làm")) {
                            isFutureExpanded = !isFutureExpanded;
                        } else if (headerTitle.startsWith("Đã hoàn thành")) {
                            isCompletedExpanded = !isCompletedExpanded;
                        }
                        loadTasks(); // Reload để ẩn/hiện item
                    }
                }
        );
        rvTasks.setAdapter(taskAdapter);
    }

    private void loadTasks() {
        if (userId == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> allTasks = db.taskDao().getAllTasks(userId);
            List<Task> filteredByCategory = new ArrayList<>();

            // 1. Lọc theo Category
            for (Task t : allTasks) {
                if (currentCategory.equals("Tất Cả") ||
                        (t.category != null && t.category.equalsIgnoreCase(currentCategory))) {
                    filteredByCategory.add(t);
                }
            }

            // 2. Phân loại vào 3 nhóm
            List<Task> overdueList = new ArrayList<>();
            List<Task> futureList = new ArrayList<>();
            List<Task> completedList = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (Task t : filteredByCategory) {
                // Logic bộ lọc 3 chấm
                boolean isTaskCompleted = t.isCompleted;
                boolean isTaskOverdue = !t.isCompleted && t.dueDate < now;
                boolean isTaskFuture = !t.isCompleted && t.dueDate >= now;

                // Áp dụng bộ lọc
                if (isTaskCompleted) {
                    if (showCompleted) completedList.add(t);
                } else if (isTaskOverdue) {
                    if (showOverdue) overdueList.add(t);
                } else {
                    if (showFuture) futureList.add(t);
                }
            }

            // 3. Build list hiển thị (Wrapper)
            List<TaskAdapter.TaskItemWrapper> displayList = new ArrayList<>();

            // --- Nhóm Hết Hạn ---
            if (!overdueList.isEmpty()) {
                displayList.add(new TaskAdapter.TaskItemWrapper("Hết hạn (" + overdueList.size() + ")", isOverdueExpanded));
                if (isOverdueExpanded) {
                    for (Task t : overdueList) displayList.add(new TaskAdapter.TaskItemWrapper(t));
                }
            }

            // --- Nhóm Tương Lai ---
            if (!futureList.isEmpty()) {
                displayList.add(new TaskAdapter.TaskItemWrapper("Việc cần làm (" + futureList.size() + ")", isFutureExpanded));
                if (isFutureExpanded) {
                    for (Task t : futureList) displayList.add(new TaskAdapter.TaskItemWrapper(t));
                }
            }

            // --- Nhóm Đã Hoàn Thành ---
            if (!completedList.isEmpty()) {
                displayList.add(new TaskAdapter.TaskItemWrapper("Đã hoàn thành (" + completedList.size() + ")", isCompletedExpanded));
                if (isCompletedExpanded) {
                    for (Task t : completedList) displayList.add(new TaskAdapter.TaskItemWrapper(t));
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (displayList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvTasks.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvTasks.setVisibility(View.VISIBLE);
                        taskAdapter.setData(displayList);
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

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                TaskAdapter.TaskItemWrapper item = taskAdapter.getItem(pos);

                if (item.type == TaskAdapter.TYPE_TASK) {
                    // Hiện Dialog xác nhận
                    showDeleteConfirmationDialog(item.task, pos);
                } else {
                    // Không cho xóa Header -> Trả lại vị trí cũ
                    taskAdapter.notifyItemChanged(pos);
                }
            }
        }).attachToRecyclerView(rvTasks);
    }

    private void showDeleteConfirmationDialog(Task task, int position) {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_delete);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.BOTTOM;
            window.setAttributes(wlp);
        }

        Button btnDelete = dialog.findViewById(R.id.btn_confirm_delete);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel_delete);

        btnDelete.setOnClickListener(v -> {
            deleteTask(task);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            taskAdapter.notifyItemChanged(position); // Undo swipe
            dialog.dismiss();
        });

        dialog.setOnCancelListener(d -> taskAdapter.notifyItemChanged(position));
        dialog.show();
    }

    private void deleteTask(Task task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            db.taskDao().deleteTask(task);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã xóa!", Toast.LENGTH_SHORT).show();
                    loadTasks();
                });
            }
        });
    }

    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(getContext(), btnMoreOptions);
        popup.getMenuInflater().inflate(R.menu.menu_todoits_filter, popup.getMenu());
        popup.getMenu().findItem(R.id.action_show_completed).setChecked(showCompleted);
        popup.getMenu().findItem(R.id.action_show_future).setChecked(showFuture);
        popup.getMenu().findItem(R.id.action_show_overdue).setChecked(showOverdue);

        popup.setOnMenuItemClickListener(item -> {
            item.setChecked(!item.isChecked());
            int id = item.getItemId();
            if (id == R.id.action_show_completed) showCompleted = item.isChecked();
            else if (id == R.id.action_show_future) showFuture = item.isChecked();
            else if (id == R.id.action_show_overdue) showOverdue = item.isChecked();
            loadTasks();
            return true;
        });
        popup.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }
}