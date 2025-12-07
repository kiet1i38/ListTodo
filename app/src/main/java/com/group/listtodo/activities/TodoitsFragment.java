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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.group.listtodo.R;
import com.group.listtodo.adapters.CategoryAdapter;
import com.group.listtodo.adapters.TaskAdapter;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Category;
import com.group.listtodo.models.Task;
import com.group.listtodo.utils.SessionManager;
import com.group.listtodo.utils.SyncHelper; // Auto Backup

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.PopupWindow;
import android.graphics.drawable.ColorDrawable;

public class TodoitsFragment extends Fragment {

    private RecyclerView rvTasks, rvCategories;
    private TaskAdapter taskAdapter;
    private CategoryAdapter categoryAdapter;
    private LinearLayout layoutEmpty;
    private ImageButton btnMoreOptions;
    private EditText edtSearch;
    private FloatingActionButton fab;

    private AppDatabase db;
    private String userId;

    private List<String> categoryList = new ArrayList<>(); // List tên để hiện lên Adapter
    private List<Category> categoryObjects = new ArrayList<>(); // List object để lấy ID khi xóa
    private String currentCategory = "Tất Cả";

    private String currentSearchKeyword = "";
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private boolean showCompleted = true;
    private boolean showFuture = true;
    private boolean showOverdue = true;

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

        db = AppDatabase.getInstance(getContext());
        SessionManager session = new SessionManager(getContext());
        userId = session.getUserId();

        rvTasks = view.findViewById(R.id.recycler_tasks);
        rvCategories = view.findViewById(R.id.recycler_categories);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        fab = view.findViewById(R.id.fab_add);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);
        edtSearch = view.findViewById(R.id.edt_search);

        setupCategoryRecycler();
        setupTaskRecycler();
        setupSwipeToDelete();
        setupSearchLogic();

        fab.setOnClickListener(v -> {
            AddNewTaskSheet bottomSheet = new AddNewTaskSheet(() -> loadTasks());
            bottomSheet.show(getParentFragmentManager(), "AddTask");
        });

        btnMoreOptions.setOnClickListener(v -> showFilterMenu());

        loadCategoriesFromDb();
        loadTasks();
    }

    // ========================================================================================
    // 1. LOGIC DANH MỤC (CATEGORY)
    // ========================================================================================

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

            @Override
            public void onCategoryLongClick(String category) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Xóa Danh Mục")
                        .setMessage("Bạn có chắc muốn xóa danh mục '" + category + "' không?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteCategory(category))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        rvCategories.setAdapter(categoryAdapter);
    }

    private void loadCategoriesFromDb() {
        if (userId == null) return;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Category> dbCats = db.categoryDao().getCategories(userId);

            if (dbCats.isEmpty()) {
                db.categoryDao().insert(new Category("Công Việc", userId));
                db.categoryDao().insert(new Category("Cá Nhân", userId));
                db.categoryDao().insert(new Category("Học Tập", userId));
                dbCats = db.categoryDao().getCategories(userId);
                SyncHelper.autoBackup(getContext()); 
            }

            categoryObjects.clear();
            categoryObjects.addAll(dbCats);

            categoryList.clear();
            categoryList.add("Tất Cả");
            for (Category c : dbCats) {
                categoryList.add(c.name);
            }
            categoryList.add("+");

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> categoryAdapter.notifyDataSetChanged());
            }
        });
    }

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
            String newCatName = edtName.getText().toString().trim();
            if (!newCatName.isEmpty()) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    db.categoryDao().insert(new Category(newCatName, userId));
                    SyncHelper.autoBackup(getContext());
                    loadCategoriesFromDb();
                });
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void deleteCategory(String categoryName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            for (Category c : categoryObjects) {
                if (c.name.equals(categoryName)) {
                    db.categoryDao().delete(c);
                    break;
                }
            }
            SyncHelper.autoBackup(getContext());
            loadCategoriesFromDb();

            if (currentCategory.equals(categoryName)) {
                currentCategory = "Tất Cả";
                loadTasks();
            }
        });
    }

    // ========================================================================================
    // 2. LOGIC DANH SÁCH TASK
    // ========================================================================================

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
                        if (headerTitle.startsWith("Hết hạn")) isOverdueExpanded = !isOverdueExpanded;
                        else if (headerTitle.startsWith("Việc cần làm")) isFutureExpanded = !isFutureExpanded;
                        else if (headerTitle.startsWith("Đã hoàn thành")) isCompletedExpanded = !isCompletedExpanded;
                        loadTasks();
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
            List<Task> filteredList = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (Task t : allTasks) {
                boolean matchCat = currentCategory.equals("Tất Cả") || (t.category != null && t.category.equalsIgnoreCase(currentCategory));
                boolean matchSearch = currentSearchKeyword.isEmpty() || t.title.toLowerCase().contains(currentSearchKeyword.toLowerCase());
                if (matchCat && matchSearch) filteredList.add(t);
            }

            List<Task> overdueList = new ArrayList<>();
            List<Task> futureList = new ArrayList<>();
            List<Task> completedList = new ArrayList<>();

            for (Task t : filteredList) {
                if (t.isCompleted) {
                    if (showCompleted) completedList.add(t);
                } else if (t.dueDate < now) {
                    if (showOverdue) overdueList.add(t);
                } else {
                    if (showFuture) futureList.add(t);
                }
            }

            List<TaskAdapter.TaskItemWrapper> displayList = new ArrayList<>();

            if (!overdueList.isEmpty()) {
                displayList.add(new TaskAdapter.TaskItemWrapper("Hết hạn (" + overdueList.size() + ")", isOverdueExpanded));
                if (isOverdueExpanded) for (Task t : overdueList) displayList.add(new TaskAdapter.TaskItemWrapper(t));
            }

            if (!futureList.isEmpty()) {
                displayList.add(new TaskAdapter.TaskItemWrapper("Việc cần làm (" + futureList.size() + ")", isFutureExpanded));
                if (isFutureExpanded) for (Task t : futureList) displayList.add(new TaskAdapter.TaskItemWrapper(t));
            }

            if (!completedList.isEmpty()) {
                displayList.add(new TaskAdapter.TaskItemWrapper("Đã hoàn thành (" + completedList.size() + ")", isCompletedExpanded));
                if (isCompletedExpanded) for (Task t : completedList) displayList.add(new TaskAdapter.TaskItemWrapper(t));
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
            SyncHelper.autoBackup(getContext());
            loadTasks();
        });
    }


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
                searchHandler.postDelayed(searchRunnable, 300);
            }
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
                    showDeleteConfirmationDialog(item.task, pos);
                } else {
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

        dialog.findViewById(R.id.btn_confirm_delete).setOnClickListener(v -> {
            deleteTask(task);
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btn_cancel_delete).setOnClickListener(v -> {
            taskAdapter.notifyItemChanged(position);
            dialog.dismiss();
        });

        dialog.setOnCancelListener(d -> taskAdapter.notifyItemChanged(position));
        dialog.show();
    }

    private void deleteTask(Task task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            db.taskDao().deleteTask(task);
            SyncHelper.autoBackup(getContext());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã xóa!", Toast.LENGTH_SHORT).show();
                    loadTasks();
                });
            }
        });
    }

    private void showFilterMenu() {
        // 1. Inflate Layout
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.layout_popup_filter, null);

        // 2. Tạo PopupWindow
        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Để bo góc đẹp
        popupWindow.setElevation(10);

        View itemCompleted = popupView.findViewById(R.id.item_completed);
        View itemFuture = popupView.findViewById(R.id.item_future);
        View itemOverdue = popupView.findViewById(R.id.item_overdue);

        ImageView checkCompleted = popupView.findViewById(R.id.check_completed);
        ImageView checkFuture = popupView.findViewById(R.id.check_future);
        ImageView checkOverdue = popupView.findViewById(R.id.check_overdue);

        checkCompleted.setVisibility(showCompleted ? View.VISIBLE : View.INVISIBLE);
        checkFuture.setVisibility(showFuture ? View.VISIBLE : View.INVISIBLE);
        checkOverdue.setVisibility(showOverdue ? View.VISIBLE : View.INVISIBLE);

        itemCompleted.setOnClickListener(v -> {
            showCompleted = !showCompleted;
            checkCompleted.setVisibility(showCompleted ? View.VISIBLE : View.INVISIBLE);
            loadTasks();
        });

        itemFuture.setOnClickListener(v -> {
            showFuture = !showFuture;
            checkFuture.setVisibility(showFuture ? View.VISIBLE : View.INVISIBLE);
            loadTasks();
        });

        itemOverdue.setOnClickListener(v -> {
            showOverdue = !showOverdue;
            checkOverdue.setVisibility(showOverdue ? View.VISIBLE : View.INVISIBLE);
            loadTasks();
        });

        popupWindow.showAsDropDown(btnMoreOptions, -200, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCategoriesFromDb(); 
        loadTasks();
    }
}
