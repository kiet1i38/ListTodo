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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import com.group.listtodo.receivers.AlarmReceiver;
import com.group.listtodo.utils.SessionManager; // Import

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditTaskActivity extends AppCompatActivity {

    private EditText edtTitle, edtNote;
    private TextView tvTimeLabel, tvTimeValue;
    private Button btnSave, btnDelete, btnChipDate;
    private Task currentTask;
    private AppDatabase db;
    private Calendar calendar = Calendar.getInstance();

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
        btnChipDate = findViewById(R.id.btn_chip_date);

        setupRow(R.id.row_time, R.drawable.ic_calendar, "Thời Gian", "Chọn >");
        setupRow(R.id.row_reminder, R.drawable.ic_check_circle, "Nhắc Nhở", "Không Nhắc Nhở >");
        setupRow(R.id.row_repeat, R.drawable.ic_dashboard, "Lặp Lại", "Không >");
        setupRow(R.id.row_sound, R.drawable.ic_menu, "Âm Thanh", "Không >");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupRow(int includeId, int iconRes, String label, String value) {
        View view = findViewById(includeId);
        ((ImageView) view.findViewById(R.id.img_icon)).setImageResource(iconRes);
        ((TextView) view.findViewById(R.id.tv_label)).setText(label);
        ((TextView) view.findViewById(R.id.tv_value)).setText(value);

        if (label.equals("Thời Gian")) {
            tvTimeValue = view.findViewById(R.id.tv_value);
            view.setOnClickListener(v -> showDateTimePicker());
        }
    }

    private void setupData() {
        if (currentTask != null) {
            edtTitle.setText(currentTask.title);
            edtNote.setText(currentTask.description);
            calendar.setTimeInMillis(currentTask.dueDate);
            updateTimeText();
        }
    }

    private void updateTimeText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String timeStr = sdf.format(calendar.getTime());
        tvTimeValue.setText(timeStr);
        btnChipDate.setText(timeStr);
    }

    private void setupEvents() {
        btnChipDate.setOnClickListener(v -> showDateTimePicker());

        btnSave.setOnClickListener(v -> {
            // Cập nhật thông tin
            currentTask.title = edtTitle.getText().toString();
            currentTask.description = edtNote.getText().toString();
            currentTask.dueDate = calendar.getTimeInMillis();

            // --- ĐẢM BẢO USER ID KHÔNG BỊ MẤT ---
            if (currentTask.userId == null) {
                currentTask.userId = new SessionManager(this).getUserId();
            }
            // ------------------------------------

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                db.taskDao().updateTask(currentTask);
                scheduleAlarm(currentTask);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });

        btnDelete.setOnClickListener(v -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                db.taskDao().deleteTask(currentTask);
                cancelAlarm(currentTask);
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
                updateTimeText();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void scheduleAlarm(Task task) {
        if (task.dueDate > System.currentTimeMillis()) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(this, AlarmReceiver.class);
            i.putExtra("TITLE", task.title);
            PendingIntent pi = PendingIntent.getBroadcast(this, task.id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            if (am != null) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueDate, pi);
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