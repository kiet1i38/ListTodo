package com.group.listtodo.activities;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
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
import com.group.listtodo.utils.SyncHelper;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditTaskActivity extends AppCompatActivity {

    // UI Components
    private EditText edtTitle, edtNote;
    private TextView tvTimeValue, tvReminderValue, tvRepeatValue, tvSoundValue;
    private Button btnSave, btnDelete;
    private Button btnChipDate, btnChipPriority, btnChipCategory, btnChipLocation;
    private LinearLayout layoutSubtasksContainer;

    // Data & State
    private Task currentTask;
    private AppDatabase db;
    private Calendar calendar = Calendar.getInstance();

    // Biến tạm lưu dữ liệu
    private int selectedPriority = 4;
    private String selectedCategory = "Công Việc";
    private String selectedLocation = "";
    private double selectedLat = 0;
    private double selectedLng = 0;

    // Biến cấu hình Báo thức
    private int reminderMinutes = 0; // 0 = Đúng giờ
    private int repeatCount = 0;     // 0 = Không lặp
    private String selectedSound = "sound_alarm"; // Tên file nhạc mặc định

    // Subtasks
    private List<SubtaskItem> subtaskList = new ArrayList<>();

    // Launchers
    private ActivityResultLauncher<Intent> subtaskLauncher;
    private ActivityResultLauncher<Intent> locationLauncher;

    // Audio Player
    private MediaPlayer previewPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        db = AppDatabase.getInstance(this);
        currentTask = (Task) getIntent().getSerializableExtra("task");

        // 1. Launcher nhận kết quả từ Subtask
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

        // 2. Launcher nhận kết quả từ Map
        locationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedLocation = result.getData().getStringExtra("location_name");
                selectedLat = result.getData().getDoubleExtra("lat", 0);
                selectedLng = result.getData().getDoubleExtra("lng", 0);
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

        findViewById(R.id.btn_add_subtask).setOnClickListener(v -> {
            SubtaskItem newItem = new SubtaskItem("", false);
            subtaskList.add(newItem);
            refreshSubtaskList();
            openEditSubtask(newItem, subtaskList.size() - 1);
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // --- SETUP CÁC DÒNG CÀI ĐẶT VỚI ICON ĐÚNG ---
        setupRow(R.id.row_time, R.drawable.ic_clock, "Thời Gian", "Chọn >");
        setupRow(R.id.row_reminder, R.drawable.ic_alarm, "Nhắc Nhở", "Không Nhắc >");
        setupRow(R.id.row_repeat, R.drawable.ic_repeat, "Lặp Lại", "Không >");
        setupRow(R.id.row_sound, R.drawable.ic_music, "Âm Thanh", "Mặc định >");
    }

    private void setupRow(int includeId, int iconRes, String label, String value) {
        View view = findViewById(includeId);
        if (view != null) {
            ((ImageView) view.findViewById(R.id.img_icon)).setImageResource(iconRes);
            ((TextView) view.findViewById(R.id.tv_label)).setText(label);
            TextView tvVal = view.findViewById(R.id.tv_value);
            tvVal.setText(value);

            // Gán biến và sự kiện click
            if (label.equals("Thời Gian")) {
                tvTimeValue = tvVal;
                view.setOnClickListener(v -> showDateTimePicker());
            } else if (label.equals("Nhắc Nhở")) {
                tvReminderValue = tvVal;
                view.setOnClickListener(v -> showReminderDialog());
            } else if (label.equals("Lặp Lại")) {
                tvRepeatValue = tvVal;
                view.setOnClickListener(v -> showRepeatMenu(view));
            } else if (label.equals("Âm Thanh")) {
                tvSoundValue = tvVal;
                view.setOnClickListener(v -> showSoundDialog());
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
            selectedLocation = currentTask.location != null ? currentTask.location : "";
            selectedLat = currentTask.locationLat;
            selectedLng = currentTask.locationLng;

            // Load Alarm settings
            reminderMinutes = currentTask.reminderMinutes;
            repeatCount = currentTask.repeatCount;
            selectedSound = currentTask.soundName != null ? currentTask.soundName : "sound_alarm";

            updateChipTexts();
            updateSettingsUI();
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
        btnChipLocation.setText(selectedLocation.isEmpty() ? "Địa Điểm" : selectedLocation);
    }

    private void updateSettingsUI() {
        if (tvReminderValue != null)
            tvReminderValue.setText(reminderMinutes == 0 ? "Đúng giờ >" : "Trước " + reminderMinutes + " phút >");
        if (tvRepeatValue != null)
            tvRepeatValue.setText(repeatCount == 0 ? "Không lặp >" : repeatCount + " lần >");
        if (tvSoundValue != null)
            tvSoundValue.setText(selectedSound + " >");
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

    // --- CÁC HÀM DIALOG NHẬP LIỆU ---

    private void showReminderDialog() {
        EditText edtMinutes = new EditText(this);
        edtMinutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        edtMinutes.setHint("Nhập số phút (VD: 5)");

        new AlertDialog.Builder(this)
                .setTitle("Báo trước bao lâu?")
                .setView(edtMinutes)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String s = edtMinutes.getText().toString();
                    if (!s.isEmpty()) {
                        reminderMinutes = Integer.parseInt(s);
                        updateSettingsUI();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRepeatMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 0, 0, "Không lặp");
        popup.getMenu().add(0, 1, 0, "1 lần");
        popup.getMenu().add(0, 3, 0, "3 lần");
        popup.getMenu().add(0, 5, 0, "5 lần");

        popup.setOnMenuItemClickListener(item -> {
            repeatCount = item.getItemId();
            updateSettingsUI();
            return true;
        });
        popup.show();
    }

    private void showSoundDialog() {
        // Tên file phải khớp trong res/raw
        String[] sounds = {"sound_alarm", "sound_notification", "sound_bell"};

        new AlertDialog.Builder(this)
                .setTitle("Chọn Âm Thanh")
                .setSingleChoiceItems(sounds, -1, (dialog, which) -> {
                    selectedSound = sounds[which];
                    playSoundPreview(selectedSound);
                })
                .setPositiveButton("Chọn", (dialog, which) -> {
                    updateSettingsUI();
                    stopSoundPreview();
                })
                .setNegativeButton("Hủy", (dialog, which) -> stopSoundPreview())
                .show();
    }

    private void playSoundPreview(String soundName) {
        stopSoundPreview();
        int resId = getResources().getIdentifier(soundName, "raw", getPackageName());
        if (resId != 0) {
            previewPlayer = MediaPlayer.create(this, resId);
            previewPlayer.start();
        }
    }
    private void stopSoundPreview() {
        if (previewPlayer != null) {
            previewPlayer.release();
            previewPlayer = null;
        }
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

        // Mở Map
        btnChipLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationActivity.class);
            if (selectedLat != 0 && selectedLng != 0) {
                intent.putExtra("old_lat", selectedLat);
                intent.putExtra("old_lng", selectedLng);
                intent.putExtra("old_name", selectedLocation);
            }
            locationLauncher.launch(intent);
        });

        // LƯU
        btnSave.setOnClickListener(v -> {
            currentTask.title = edtTitle.getText().toString();
            currentTask.description = edtNote.getText().toString();
            currentTask.dueDate = calendar.getTimeInMillis();
            currentTask.priority = selectedPriority;
            currentTask.category = selectedCategory;
            currentTask.location = selectedLocation;
            currentTask.locationLat = selectedLat;
            currentTask.locationLng = selectedLng;
            currentTask.subtasks = new Gson().toJson(subtaskList);

            // Lưu Alarm Config
            currentTask.reminderMinutes = reminderMinutes;
            currentTask.repeatCount = repeatCount;
            currentTask.soundName = selectedSound;

            if (currentTask.userId == null) {
                currentTask.userId = new SessionManager(this).getUserId();
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                db.taskDao().updateTask(currentTask);

                // Đặt báo thức
                scheduleAlarm(currentTask);

                // Auto Backup
                SyncHelper.autoBackup(this);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã lưu!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });

        // XÓA
        btnDelete.setOnClickListener(v -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                db.taskDao().deleteTask(currentTask);
                cancelAlarm(currentTask);
                SyncHelper.autoBackup(this);
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
                if (tvTimeValue != null) tvTimeValue.setText(new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void scheduleAlarm(Task task) {
        // Tính thời gian kích hoạt = Hạn chót - số phút nhắc
        long triggerTime = task.dueDate - (task.reminderMinutes * 60 * 1000L);

        if (triggerTime > System.currentTimeMillis()) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(this, AlarmReceiver.class);

            i.putExtra("TITLE", task.title);
            i.putExtra("ID", task.id);
            i.putExtra("SOUND", task.soundName);
            i.putExtra("REPEAT", task.repeatCount);

            PendingIntent pi = PendingIntent.getBroadcast(this, task.id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            if (am != null) {
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

    @Override
    protected void onDestroy() {
        stopSoundPreview();
        super.onDestroy();
    }
}