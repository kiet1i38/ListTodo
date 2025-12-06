package com.group.listtodo.activities;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
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
import com.group.listtodo.models.SubtaskItem;
import com.group.listtodo.models.Task;
import com.group.listtodo.receivers.AlarmReceiver;
import com.group.listtodo.utils.SessionManager;
import com.group.listtodo.utils.SyncHelper; // Import Auto Backup

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
    private Button btnChipDate, btnChipPriority, btnChipCategory, btnChipLocation;
    private LinearLayout layoutSubtasksContainer;

    private Task currentTask;
    private AppDatabase db;
    private Calendar calendar = Calendar.getInstance();

    // Các biến dữ liệu tạm thời
    private int selectedPriority = 4;
    private String selectedCategory = "Công Việc";
    private String selectedLocation = "";
    private double selectedLat = 0;
    private double selectedLng = 0;

    private List<SubtaskItem> subtaskList = new ArrayList<>();
    private ActivityResultLauncher<Intent> subtaskLauncher;
    private ActivityResultLauncher<Intent> locationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        db = AppDatabase.getInstance(this);
        currentTask = (Task) getIntent().getSerializableExtra("task");

        // 1. LAUNCHER NHẬN KẾT QUẢ TỪ SUBTASK (Sửa/Xóa)
        subtaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                SubtaskItem updatedItem = (SubtaskItem) result.getData().getSerializableExtra("updated_subtask");
                int pos = result.getData().getIntExtra("position", -1);
                if (pos >= 0 && pos < subtaskList.size()) {
                    subtaskList.set(pos, updatedItem);
                    refreshSubtaskList();
                }
            } else if (result.getResultCode() == RESULT_FIRST_USER && result.getData() != null) {
                int pos = result.getData().getIntExtra("delete_position", -1);
                if (pos >= 0 && pos < subtaskList.size()) {
                    subtaskList.remove(pos);
                    refreshSubtaskList();
                }
            }
        });

        // 2. LAUNCHER NHẬN KẾT QUẢ TỪ BẢN ĐỒ (Địa điểm)
        locationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedLocation = result.getData().getStringExtra("location_name");
                selectedLat = result.getData().getDoubleExtra("lat", 0);
                selectedLng = result.getData().getDoubleExtra("lng", 0);

                // Cập nhật nút bấm ngay lập tức
                btnChipLocation.setText(selectedLocation);
            }
        });

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
        btnChipLocation = findViewById(R.id.btn_chip_location);

        layoutSubtasksContainer = findViewById(R.id.layout_subtasks_container);

        // Nút thêm Subtask mới
        findViewById(R.id.btn_add_subtask).setOnClickListener(v -> {
            SubtaskItem newItem = new SubtaskItem("", false);
            subtaskList.add(newItem);
            refreshSubtaskList();
            openEditSubtask(newItem, subtaskList.size() - 1);
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Setup các dòng cài đặt (Fix lỗi hiển thị "Label")
        setupRow(R.id.row_time, R.drawable.ic_clock, "Thời Gian", "Chọn >");
        setupRow(R.id.row_reminder, R.drawable.ic_alarm, "Nhắc Nhở", "Không Nhắc >");
        setupRow(R.id.row_repeat, R.drawable.ic_repeat, "Lặp Lại", "Không >");
        setupRow(R.id.row_sound, R.drawable.ic_music, "Âm Thanh", "Không >");
    }

    private void setupRow(int includeId, int iconRes, String label, String value) {
        View view = findViewById(includeId);
        if (view != null) {
            ((ImageView) view.findViewById(R.id.img_icon)).setImageResource(iconRes);
            ((TextView) view.findViewById(R.id.tv_label)).setText(label);
            ((TextView) view.findViewById(R.id.tv_value)).setText(value);

            // Nếu là dòng Thời Gian thì gán sự kiện click để chọn giờ
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

            // Load địa điểm và tọa độ từ DB
            selectedLocation = currentTask.location != null ? currentTask.location : "";
            selectedLat = currentTask.locationLat;
            selectedLng = currentTask.locationLng;

            updateChipTexts();
            loadSubtasks();
        }
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

        // Hiển thị tên địa điểm hoặc mặc định
        btnChipLocation.setText(selectedLocation.isEmpty() ? "Địa Điểm" : selectedLocation);
    }

    private void loadSubtasks() {
        if (currentTask.subtasks != null && !currentTask.subtasks.isEmpty()) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<SubtaskItem>>(){}.getType();
            List<SubtaskItem> list = gson.fromJson(currentTask.subtasks, listType);
            if (list != null) {
                subtaskList.clear();
                subtaskList.addAll(list);
            }
            refreshSubtaskList();
        }
    }

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

        // Không cho nhập trực tiếp, bấm vào để mở màn hình EditSubtask
        edt.setFocusable(false);
        edt.setClickable(true);
        edt.setOnClickListener(v -> openEditSubtask(item, position));

        cb.setOnClickListener(v -> item.isCompleted = cb.isChecked());

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
            popup.setOnMenuItemClickListener(item -> {
                selectedCategory = item.getTitle().toString();
                updateChipTexts();
                return true;
            });
            popup.show();
        });

        // --- SỰ KIỆN MỞ BẢN ĐỒ ---
        btnChipLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationActivity.class);
            // Gửi tọa độ cũ sang để bản đồ hiển thị đúng marker
            if (selectedLat != 0 && selectedLng != 0) {
                intent.putExtra("old_lat", selectedLat);
                intent.putExtra("old_lng", selectedLng);
                intent.putExtra("old_name", selectedLocation);
            }
            locationLauncher.launch(intent);
        });

        // --- SỰ KIỆN LƯU ---
        btnSave.setOnClickListener(v -> {
            currentTask.title = edtTitle.getText().toString();
            currentTask.description = edtNote.getText().toString();
            currentTask.dueDate = calendar.getTimeInMillis();
            currentTask.priority = selectedPriority;
            currentTask.category = selectedCategory;

            // Lưu địa điểm và tọa độ
            currentTask.location = selectedLocation;
            currentTask.locationLat = selectedLat;
            currentTask.locationLng = selectedLng;

            currentTask.subtasks = new Gson().toJson(subtaskList);

            if (currentTask.userId == null) {
                currentTask.userId = new SessionManager(this).getUserId();
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                db.taskDao().updateTask(currentTask);

                // Đặt lại báo thức
                scheduleAlarm(currentTask);

                // Tự động sao lưu lên Cloud
                SyncHelper.autoBackup(this);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });

        // --- SỰ KIỆN XÓA ---
        btnDelete.setOnClickListener(v -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                db.taskDao().deleteTask(currentTask);

                // Hủy báo thức
                cancelAlarm(currentTask);

                // Tự động sao lưu
                SyncHelper.autoBackup(this);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã xóa công việc!", Toast.LENGTH_SHORT).show();
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

    private void scheduleAlarm(Task task) {
        // Tính toán thời gian nhắc: Hạn chót - 1 ngày (24h * 60p * 60s * 1000ms)
        long oneDayInMillis = 24 * 60 * 60 * 1000;
        long triggerTime = task.dueDate - oneDayInMillis;


        if (triggerTime > System.currentTimeMillis()) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(this, AlarmReceiver.class);

            // Sửa tiêu đề thông báo chút cho hợp lý
            i.putExtra("TITLE", "Sắp đến hạn (còn 1 ngày): " + task.title);

            PendingIntent pi = PendingIntent.getBroadcast(this, task.id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            if (am != null) {
                // Đặt lịch vào đúng thời điểm triggerTime đã tính
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            }
        }
    }

    private void cancelAlarm(Task task) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, task.id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (pi != null && am != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }
}