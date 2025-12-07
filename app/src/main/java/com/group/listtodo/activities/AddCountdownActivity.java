package com.group.listtodo.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.utils.SessionManager; // <--- Import quan trọng
import com.group.listtodo.utils.SyncHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddCountdownActivity extends AppCompatActivity {

    private EditText edtTitle;
    private TextView tvDateValue, tvScreenTitle;
    private Button btnSaveTop, btnSaveBottom, btnDelete;
    // Đã xóa Switch switchPin

    private Calendar calendar = Calendar.getInstance();
    private AppDatabase db;
    private CountdownEvent currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_countdown);

        db = AppDatabase.getInstance(this);
        currentEvent = (CountdownEvent) getIntent().getSerializableExtra("event");

        initViews();
        setupData();
        setupEvents();
    }

    private void initViews() {
        edtTitle = findViewById(R.id.edt_title);
        tvDateValue = findViewById(R.id.tv_date_value);
        tvScreenTitle = findViewById(R.id.tv_screen_title);

        btnSaveTop = findViewById(R.id.btn_save_top);
        btnSaveBottom = findViewById(R.id.btn_save_bottom);
        btnDelete = findViewById(R.id.btn_delete);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupData() {
        if (currentEvent != null) {
            tvScreenTitle.setText("Chỉnh Sửa Sự Kiện");
            edtTitle.setText(currentEvent.title);
            calendar.setTimeInMillis(currentEvent.targetDate);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            tvScreenTitle.setText("Thêm Sự Kiện Nhỏ");
            btnDelete.setVisibility(View.GONE);
        }
        updateDateText();
    }

    private void setupEvents() {
        findViewById(R.id.row_date).setOnClickListener(v -> showDatePicker());

        View.OnClickListener saveAction = v -> saveEvent();
        btnSaveTop.setOnClickListener(saveAction);
        btnSaveBottom.setOnClickListener(saveAction);

        btnDelete.setOnClickListener(v -> deleteEvent());
    }

    private void showDatePicker() {
        // Sử dụng CustomCalendarBottomSheet thay vì DatePickerDialog cũ
        CustomCalendarBottomSheet calendarSheet = new CustomCalendarBottomSheet(calendar.getTimeInMillis(), dateInMillis -> {
            calendar.setTimeInMillis(dateInMillis);
            updateDateText();
        });
        calendarSheet.show(getSupportFragmentManager(), "CalendarSheet");
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
        tvDateValue.setText(sdf.format(calendar.getTime()) + " >");
    }

    private void saveEvent() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEvent == null) {
            currentEvent = new CountdownEvent();
        }

        // 1. Cập nhật thông tin
        currentEvent.title = title;
        currentEvent.targetDate = calendar.getTimeInMillis();

        // 2. QUAN TRỌNG: Gán UserID của người đang đăng nhập
        SessionManager session = new SessionManager(this);
        currentEvent.userId = session.getUserId();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (currentEvent.id == 0) {
                db.countdownDao().insert(currentEvent);
            } else {
                db.countdownDao().update(currentEvent);
            }
            com.group.listtodo.utils.SyncHelper.autoBackup(this);

            runOnUiThread(() -> {
                Toast.makeText(this, "Đã lưu thành công!", Toast.LENGTH_SHORT).show();
                SyncHelper.autoBackup(this);
                finish(); // Đóng màn hình, quay về danh sách
            });
        });
    }

    private void deleteEvent() {
        if (currentEvent == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            db.countdownDao().delete(currentEvent);
            com.group.listtodo.utils.SyncHelper.autoBackup(this);
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xóa sự kiện!", Toast.LENGTH_SHORT).show();
                SyncHelper.autoBackup(this);
                finish();
            });
        });
    }
}