package com.group.listtodo.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTaskActivity extends AppCompatActivity {

    private EditText edtTitle, edtDesc;
    private TextView tvSelectedDate;
    private RadioGroup rgPriority;
    private Calendar calendar = Calendar.getInstance();
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        db = AppDatabase.getInstance(this);

        // Init Views
        edtTitle = findViewById(R.id.edt_title);
        edtDesc = findViewById(R.id.edt_desc);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        rgPriority = findViewById(R.id.rg_priority);
        Button btnDate = findViewById(R.id.btn_pick_date);
        Button btnSave = findViewById(R.id.btn_save);

        // 1. Xử lý chọn ngày giờ (Advanced GUI: Dialogs)
        btnDate.setOnClickListener(v -> showDateTimePicker());

        // 2. Xử lý lưu (Storage + Multi-threading)
        btnSave.setOnClickListener(v -> saveTask());
    }

    private void showDateTimePicker() {
        // Chọn ngày
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            // Chọn giờ sau khi chọn ngày
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                tvSelectedDate.setText(calendar.getTime().toString());
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveTask() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên công việc!", Toast.LENGTH_SHORT).show();
            return;
        }

        int priority = 4;
        int selectedId = rgPriority.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_prio_1) priority = 1;
        else if (selectedId == R.id.rb_prio_2) priority = 2;
        else if (selectedId == R.id.rb_prio_3) priority = 3;

        Task task = new Task(title, calendar.getTimeInMillis(), priority, "Personal");
        task.description = edtDesc.getText().toString();

        // CHẠY TRÊN BACKGROUND THREAD (Requirement: Multi-threading)
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            db.taskDao().insertTask(task);

            // Update UI trên Main Thread
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã thêm công việc!", Toast.LENGTH_SHORT).show();
                finish(); // Đóng Activity quay về màn hình chính
            });
        });
    }
}
