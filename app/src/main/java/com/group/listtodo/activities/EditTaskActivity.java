package com.group.listtodo.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.SubtaskItem; // Import Model mới
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
    private TextView tvTimeValue;
    private Button btnSave, btnDelete;
    private Button btnChipDate, btnChipPriority, btnChipCategory;
    private LinearLayout layoutSubtasksContainer;

    private Task currentTask;
    private AppDatabase db;
    private Calendar calendar = Calendar.getInstance();
    private int selectedPriority = 4;
    private String selectedCategory = "Công Việc";

    // Danh sách Subtask trong bộ nhớ
    private List<SubtaskItem> subtaskList = new ArrayList<>();

    // Launcher để nhận kết quả từ EditSubtaskActivity
    private ActivityResultLauncher<Intent> subtaskLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        db = AppDatabase.getInstance(this);
        currentTask = (Task) getIntent().getSerializableExtra("task");

        // Đăng ký Launcher nhận kết quả
        subtaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Cập nhật Subtask
                        SubtaskItem updatedItem = (SubtaskItem) result.getData().getSerializableExtra("updated_subtask");
                        int pos = result.getData().getIntExtra("position", -1);
                        if (pos >= 0 && pos < subtaskList.size()) {
                            subtaskList.set(pos, updatedItem);
                            refreshSubtaskList(); // Vẽ lại list
                        }
                    } else if (result.getResultCode() == RESULT_FIRST_USER && result.getData() != null) {
                        // Xóa Subtask
                        int pos = result.getData().getIntExtra("delete_position", -1);
                        if (pos >= 0 && pos < subtaskList.size()) {
                            subtaskList.remove(pos);
                            refreshSubtaskList();
                        }
                    }
                }
        );

        initViews();
        setupData();
        setupEvents();
    }

    private void initViews() {
        edtTitle = findViewById(R.id.edt_task_title);
        edtNote = findViewById(R.id.edt_note);
        btnSave = findViewById(R.id.btn_save_changes);
        btnDelete = findViewById(R.id.btn_delete);
        btnChipDate = findViewById(R.id.btn_chip_date);
        btnChipPriority = findViewById(R.id.btn_chip_priority);
        btnChipCategory = findViewById(R.id.btn_chip_category);
        layoutSubtasksContainer = findViewById(R.id.layout_subtasks_container);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Thêm subtask mới
        findViewById(R.id.btn_add_subtask).setOnClickListener(v -> {
            SubtaskItem newItem = new SubtaskItem("", false);
            subtaskList.add(newItem);
            refreshSubtaskList();

            // Mở luôn màn hình edit cho item mới tạo
            openEditSubtask(newItem, subtaskList.size() - 1);
        });

        setupRow(R.id.row_time, R.drawable.ic_calendar, "Thời Gian", "Chọn >");
        setupRow(R.id.row_reminder, R.drawable.ic_check_circle, "Nhắc Nhở", "Không Nhắc Nhở >");
        setupRow(R.id.row_repeat, R.drawable.ic_dashboard, "Lặp Lại", "Không >");
        setupRow(R.id.row_sound, R.drawable.ic_menu, "Âm Thanh", "Không >");
    }

    private void setupRow(int includeId, int iconRes, String label, String value) {
        View view = findViewById(includeId);
        if (view != null) {
            ((ImageView) view.findViewById(R.id.img_icon)).setImageResource(iconRes);
            ((TextView) view.findViewById(R.id.tv_label)).setText(label);
            ((TextView) view.findViewById(R.id.tv_value)).setText(value);
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

            // Load Subtasks từ JSON
            if (currentTask.subtasks != null && !currentTask.subtasks.isEmpty()) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<SubtaskItem>>(){}.getType();
                List<SubtaskItem> list = gson.fromJson(currentTask.subtasks, listType);
                if (list != null) subtaskList.addAll(list);
            }
            refreshSubtaskList();
        }
    }

    // Vẽ lại toàn bộ list subtask
    private void refreshSubtaskList() {
        layoutSubtasksContainer.removeAllViews();
        for (int i = 0; i < subtaskList.size(); i++) {
            addSubtaskView(subtaskList.get(i), i);
        }
    }

    private void addSubtaskView(SubtaskItem item, int position) {
        View view = getLayoutInflater().inflate(R.layout.item_subtask_edit, layoutSubtasksContainer, false);

        CheckBox cb = view.findViewById(R.id.cb_subtask);
        EditText edt = view.findViewById(R.id.edt_subtask_title);
        ImageView btnRemove = view.findViewById(R.id.btn_remove_subtask);

        cb.setChecked(item.isCompleted);
        edt.setText(item.title);

        // Disable EditText để bắt sự kiện click mở màn hình Edit
        edt.setFocusable(false);
        edt.setClickable(true);
        edt.setOnClickListener(v -> openEditSubtask(item, position));

        // Sự kiện Checkbox
        cb.setOnClickListener(v -> item.isCompleted = cb.isChecked());

        // Sự kiện Xóa nhanh
        btnRemove.setOnClickListener(v -> {
            subtaskList.remove(position);
            refreshSubtaskList();
        });

        layoutSubtasksContainer.addView(view);
    }

    private void openEditSubtask(SubtaskItem item, int position) {
        Intent intent = new Intent(this, EditSubtaskActivity.class);
        intent.putExtra("subtask", item);
        intent.putExtra("position", position);
        subtaskLauncher.launch(intent);
    }

    private void updateChipTexts() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        String timeStr = sdf.format(calendar.getTime());
        btnChipDate.setText(timeStr);
        if (tvTimeValue != null) tvTimeValue.setText(timeStr);

        String prioText = "Bình thường";
        if (selectedPriority == 1) prioText = "Khẩn & QT";
        else if (selectedPriority == 2) prioText = "Quan trọng";
        else if (selectedPriority == 3) prioText = "Khẩn cấp";
        btnChipPriority.setText(prioText);

        btnChipCategory.setText(selectedCategory);
    }

    private void setupEvents() {
        btnChipDate.setOnClickListener(v -> showDateTimePicker());

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

        btnSave.setOnClickListener(v -> {
            currentTask.title = edtTitle.getText().toString();
            currentTask.description = edtNote.getText().toString();
            currentTask.dueDate = calendar.getTimeInMillis();
            currentTask.priority = selectedPriority;
            currentTask.category = selectedCategory;

            // Chuyển List Subtask thành JSON để lưu
            currentTask.subtasks = new Gson().toJson(subtaskList);

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