package com.group.listtodo.activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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

    // View Components
    private RecyclerView rvTasks, rvCategories;
    private TaskAdapter taskAdapter;
    private CategoryAdapter categoryAdapter;
    private AppDatabase db;
    private LinearLayout layoutEmpty;
    private ImageButton btnMoreOptions;
    private EditText edtSearch;

    // Data & State
    private String userId;
    private List<String> categoryList = new ArrayList<>(Arrays.asList("Tất Cả", "Công Việc", "Cá Nhân", "Học Tập", "+"));
    private String currentCategory = "Tất Cả";
    private String currentSearchKeyword = "";

    // Search Debounce
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Filter Flags (Menu 3 chấm)
    private boolean showCompleted = true;
    private boolean showFuture = true;
    private boolean showOverdue = true;

    // Section Expand Flags (Đóng/Mở nhóm)
    private boolean isOverdueExpanded = true;
    private boolean isFutureExpanded = true;
    private boolean isCompletedExpanded = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_todoits, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Init Data
        db = AppDatabase.getInstance(getContext());
        SessionManager session = new SessionManager(getContext());
        userId = session.getUserId();

        // 2. Bind Views
        rvTasks = view.findViewById(R.id.recycler_tasks);
        rvCategories = view.findViewById(R.id.recycler_categories);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);
        edtSearch = view.findViewById(R.id.edt_search);

        // 3. Setup Components
        setupCategoryRecycler();
        setupTaskRecycler();
        setupSwipeToDelete();
        setupSearchLogic();

        // 4. Events
        fab.setOnClickListener(v -> {
            AddNewTaskSheet bottomSheet = new AddNewTaskSheet(() -> loadTasks());
            bottomSheet.show(getParentFragmentManager(), "AddTask");
        });

        btnMoreOptions.setOnClickListener(v -> showFilterMenu());

        // 5. Initial Load
        loadTasks();
    }

    // --- SETUP TÌM KIẾM (DEBOUNCE) ---
    private void setupSearchLogic() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }
            @Override public void afterTextChanged(Editable s) {
                searchRunnable = () -> {
                    currentSearchKeyword = s.toString().trim();
                    loadTasks();
                };
                searchHandler.postDelayed(searchRunnable, 300); // Đợi 300ms mới tìm
            }
        });
    }

    // --- SETUP DANH MỤC NGANG ---
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

    // --- DIALOG THÊM DANH MỤC (CUSTOM) ---
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
                // Thêm vào trước dấu "+" (vị trí size - 1)
                categoryList.add(categoryList.size() - 1, newCat);
                categoryAdapter.notifyDataSetChanged();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // --- SETUP DANH SÁCH CÔNG VIỆC ---
    private void setupTaskRecycler() {
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new TaskAdapter(
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
                new TaskAdapter.OnHeaderClickListener() {
                    @Override
                    public void onHeaderClick(String headerTitle) {
                        // Logic đóng mở nhóm
                        if (headerTitle.startsWith("Hết hạn")) isOverdueExpanded = !isOverdueExpanded;
                        else if (headerTitle.startsWith("Việc cần làm")) isFutureExpanded = !isFutureExpanded;
                        else if (headerTitle.startsWith("Đã hoàn thành")) isCompletedExpanded = !isCompletedExpanded;

                        loadTasks(); // Reload để áp dụng trạng thái mới
                    }
                }
        );
        rvTasks.setAdapter(taskAdapter);
    }

    // --- LOGIC LOAD DỮ LIỆU CHÍNH ---
    private void loadTasks() {
        if (userId == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> allTasks = db.taskDao().getAllTasks(userId);
            List<Task> filteredList = new ArrayList<>();

            // BƯỚC 1: LỌC (Danh mục + Tìm kiếm)
            for (Task t : allTasks) {
                boolean matchCategory = currentCategory.equals("Tất Cả") ||
                        (t.category != null && t.category.equalsIgnoreCase(currentCategory));

                boolean matchSearch = currentSearchKeyword.isEmpty() ||
                        t.title.toLowerCase().contains(currentSearchKeyword.toLowerCase());

                if (matchCategory && matchSearch) {
                    filteredList.add(t);
                }
            }

            // BƯỚC 2: PHÂN NHÓM (Overdue, Future, Completed)
            List<Task> overdueList = new ArrayList<>();
            List<Task> futureList = new ArrayList<>();
            List<Task> completedList = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (Task t : filteredList) {
                boolean isTaskCompleted = t.isCompleted;
                boolean isTaskOverdue = !t.isCompleted && t.dueDate < now;
                boolean isTaskFuture = !t.isCompleted && t.dueDate >= now;

                // Áp dụng bộ lọc 3 chấm
                if (isTaskCompleted) {
                    if (showCompleted) completedList.add(t);
                } else if (isTaskOverdue) {
                    if (showOverdue) overdueList.add(t);
                } else {
                    if (showFuture) futureList.add(t);
                }
            }

            // BƯỚC 3: BUILD DISPLAY LIST (Header + Items)
            List<TaskAdapter.TaskItemWrapper> displayList = new ArrayList<>();

            // Nhóm Hết hạn
            if (!overdueList.isEmpty()) {
                displayList.add(new TaskAdapter.TaskItemWrapper("Hết hạn (" + overdueList.size() + ")", isOverdueExpanded));
                if (isOverdueExpanded) {
                    for (Task t : overdueList) displayList.add(new TaskAdapter.TaskItemWrapper(t));
                }
            }

            // Nhóm Tương lai
            if (!futureList.isEmpty()) {
                displayList.add(new TaskAdapter.TaskItemWrapper("Việc cần làm (" + futureList.size() + ")", isFutureExpanded));
                if (isFutureExpanded) {
                    for (Task t : futureList) displayList.add(new TaskAdapter.TaskItemWrapper(t));
                }
            }

            // Nhóm Đã xong
            if (!completedList.isEmpty()) {
                displayList.add(new TaskAdapter.TaskItemWrapper("Đã hoàn thành (" + completedList.size() + ")", isCompletedExpanded));
                if (isCompletedExpanded) {
                    for (Task t : completedList) displayList.add(new TaskAdapter.TaskItemWrapper(t));
                }
            }

            // BƯỚC 4: UPDATE UI
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

    // --- LOGIC VUỐT ĐỂ XÓA ---
    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                TaskAdapter.TaskItemWrapper item = taskAdapter.getItem(pos);

                if (item.type == TaskAdapter.TYPE_TASK) {
                    showDeleteConfirmationDialog(item.task, pos); // Hiện dialog xác nhận
                } else {
                    taskAdapter.notifyItemChanged(pos); // Không cho xóa Header
                }
            }
        }).attachToRecyclerView(rvTasks);
    }

    // --- DIALOG XÁC NHẬN XÓA ---
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

        View btnDelete = dialog.findViewById(R.id.btn_confirm_delete);
        View btnCancel = dialog.findViewById(R.id.btn_cancel_delete);

        btnDelete.setOnClickListener(v -> {
            deleteTask(task);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            taskAdapter.notifyItemChanged(position); // Hoàn tác vuốt
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

    // --- MENU LỌC 3 CHẤM ---
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