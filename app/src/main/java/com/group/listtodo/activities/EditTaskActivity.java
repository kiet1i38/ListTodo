package com.group.listtodo.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import com.group.listtodo.utils.SessionManager;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditTaskActivity extends AppCompatActivity {

    private EditText edtTitle, edtNote;
    private TextView tvTimeValue; // Biến để hiển thị giờ
    private Button btnSave, btnDelete;
    private Button btnChipDate, btnChipPriority, btnChipCategory;
    private LinearLayout layoutSubtasksContainer;

    private Task currentTask;
    private AppDatabase db;
    private Calendar calendar = Calendar.getInstance();

    // Dữ liệu tạm thời
    private int selectedPriority = 4;
    private String selectedCategory = "Công Việc";

    // Class nội bộ để lưu Subtask
    public static class SubtaskItem {
        public String title;
        public boolean isCompleted;
        public SubtaskItem(String title, boolean isCompleted) {
            this.title = title;
            this.isCompleted = isCompleted;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        db = AppDatabase.getInstance(this);
        currentTask = (Task) getIntent().getSerializableExtra("task");

        initViews();
        setupData();
        setupEvents();
    }

    private void initViews() {
        edtTitle = findViewById(R.id.edt_task_title);
        edtNote = findViewById(R.id.edt_note);
        btnSave = findViewById(R.id.btn_save_changes);
        btnDelete = findViewById(R.id.btn_delete);

        // Chips
        btnChipDate = findViewById(R.id.btn_chip_date);
        btnChipPriority = findViewById(R.id.btn_chip_priority);
        btnChipCategory = findViewById(R.id.btn_chip_category);

        // Subtasks Container
        layoutSubtasksContainer = findViewById(R.id.layout_subtasks_container);
        findViewById(R.id.btn_add_subtask).setOnClickListener(v -> addSubtaskView("", false));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // --- ĐÂY LÀ PHẦN QUAN TRỌNG ĐỂ SỬA CHỮ "LABEL" ---
        // Gọi hàm setupRow để thay đổi icon và text cho từng dòng
        setupRow(R.id.row_time, R.drawable.ic_calendar, "Thời Gian", "Chọn >");
        setupRow(R.id.row_reminder, R.drawable.ic_check_circle, "Nhắc Nhở", "Không Nhắc Nhở >");
        setupRow(R.id.row_repeat, R.drawable.ic_dashboard, "Lặp Lại", "Không >");
        setupRow(R.id.row_sound, R.drawable.ic_menu, "Âm Thanh", "Không >");
        // -------------------------------------------------
    }

    // Hàm này sẽ tìm vào bên trong thẻ <include> để sửa chữ và hình
    private void setupRow(int includeId, int iconRes, String label, String value) {
        View view = findViewById(includeId);
        if (view != null) {
            ((ImageView) view.findViewById(R.id.img_icon)).setImageResource(iconRes);
            ((TextView) view.findViewById(R.id.tv_label)).setText(label);
            ((TextView) view.findViewById(R.id.tv_value)).setText(value);

            // Nếu là dòng Thời Gian thì gán biến để tí nữa update giờ
            if (label.equals("Thời Gian")) {
                tvTimeValue = view.findViewById(R.id.tv_value);
                view.setOnClickListener(v -> showDateTimePicker());
            }
        }
    }

    private void setupData() {
        if (currentTask != null) {
            edtTitle.setText(currentTask.title);
            edtNote.setText(currentTask.description);
            calendar.setTimeInMillis(currentTask.dueDate);
            selectedPriority = currentTask.priority;
            selectedCategory = currentTask.category != null ? currentTask.category : "Công Việc";

            updateChipTexts();
            loadSubtasks();
        }
    }

    private void updateChipTexts() {
        // Date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        String timeStr = sdf.format(calendar.getTime());
        btnChipDate.setText(timeStr);

        // Cập nhật cả dòng text ở dưới phần cài đặt
        if (tvTimeValue != null) {
            tvTimeValue.setText(timeStr);
        }

        // Priority
        String prioText = "Bình thường";
        if (selectedPriority == 1) prioText = "Khẩn & QT";
        else if (selectedPriority == 2) prioText = "Quan trọng";
        else if (selectedPriority == 3) prioText = "Khẩn cấp";
        btnChipPriority.setText(prioText);

        // Category
        btnChipCategory.setText(selectedCategory);
    }

    private void loadSubtasks() {
        if (currentTask.subtasks != null && !currentTask.subtasks.isEmpty()) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<SubtaskItem>>(){}.getType();
            List<SubtaskItem> list = gson.fromJson(currentTask.subtasks, listType);

            if (list != null) {
                for (SubtaskItem item : list) {
                    addSubtaskView(item.title, item.isCompleted);
                }
            }
        }
    }

    private void addSubtaskView(String title, boolean isCompleted) {
        View view = getLayoutInflater().inflate(R.layout.item_subtask_edit, layoutSubtasksContainer, false);

        CheckBox cb = view.findViewById(R.id.cb_subtask);
        EditText edt = view.findViewById(R.id.edt_subtask_title);
        ImageView btnRemove = view.findViewById(R.id.btn_remove_subtask);

        cb.setChecked(isCompleted);
        edt.setText(title);

        btnRemove.setOnClickListener(v -> layoutSubtasksContainer.removeView(view));

        layoutSubtasksContainer.addView(view);
    }

    private String getSubtasksJson() {
        List<SubtaskItem> list = new ArrayList<>();
        for (int i = 0; i < layoutSubtasksContainer.getChildCount(); i++) {
            View view = layoutSubtasksContainer.getChildAt(i);
            CheckBox cb = view.findViewById(R.id.cb_subtask);
            EditText edt = view.findViewById(R.id.edt_subtask_title);

            String text = edt.getText().toString().trim();
            if (!text.isEmpty()) {
                list.add(new SubtaskItem(text, cb.isChecked()));
            }
        }
        return new Gson().toJson(list);
    }

    private void setupEvents() {
        // 1. Date Picker
        btnChipDate.setOnClickListener(v -> showDateTimePicker());

        // 2. Priority Menu
        btnChipPriority.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, btnChipPriority);
            popup.getMenu().add(0, 1, 0, "🔴 Khẩn cấp & Quan trọng");
            popup.getMenu().add(0, 2, 0, "🟠 Quan trọng");
            popup.getMenu().add(0, 3, 0, "🔵 Khẩn cấp");
            popup.getMenu().add(0, 4, 0, "🟢 Bình thường");
            popup.setOnMenuItemClickListener(item -> {
                selectedPriority = item.getItemId();
                updateChipTexts();
                return true;
            });
            popup.show();
        });

        // 3. Category Menu
        btnChipCategory.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, btnChipCategory);
            popup.getMenu().add("Công Việc");
            popup.getMenu().add("Cá Nhân");
            popup.getMenu().add("Học Tập");
            popup.getMenu().add("Gia Đình");
            popup.setOnMenuItemClickListener(item -> {
                selectedCategory = item.getTitle().toString();
                updateChipTexts();
                return true;
            });
            popup.show();
        });

        // 4. Save
        btnSave.setOnClickListener(v -> {
            currentTask.title = edtTitle.getText().toString();
            currentTask.description = edtNote.getText().toString();
            currentTask.dueDate = calendar.getTimeInMillis();
            currentTask.priority = selectedPriority;
            currentTask.category = selectedCategory;
            currentTask.subtasks = getSubtasksJson(); // Lưu subtask

            if (currentTask.userId == null) {
                currentTask.userId = new SessionManager(this).getUserId();
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                db.taskDao().updateTask(currentTask);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã lưu!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });

        // 5. Delete
        btnDelete.setOnClickListener(v -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                db.taskDao().deleteTask(currentTask);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });
    }

    private void showDateTimePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                updateChipTexts();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}