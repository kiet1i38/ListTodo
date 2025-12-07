package com.group.listtodo.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.utils.SessionManager;
import com.group.listtodo.utils.SyncHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddCountdownActivity extends AppCompatActivity {

    private EditText edtTitle;
    private TextView tvDateValue, tvScreenTitle, tvCategoryValue, tvReminderValue;
    private Button btnSaveTop, btnSaveBottom, btnDelete;

    private Calendar calendar = Calendar.getInstance();
    private AppDatabase db;
    private CountdownEvent currentEvent;

    private String selectedCategory = "Cuộc Sống";
    private int reminderMinutes = 0;

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

        // Mới thêm
        tvCategoryValue = findViewById(R.id.tv_category_value);
        tvReminderValue = findViewById(R.id.tv_reminder_value);

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

            selectedCategory = currentEvent.category != null ? currentEvent.category : "Cuộc Sống";
            reminderMinutes = currentEvent.reminderMinutes;

            btnDelete.setVisibility(View.VISIBLE);
        } else {
            tvScreenTitle.setText("Thêm Sự Kiện Nhỏ");
            btnDelete.setVisibility(View.GONE);
        }

        updateUI();
    }

    private void updateUI() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
        tvDateValue.setText(sdf.format(calendar.getTime()) + " >");

        tvCategoryValue.setText(selectedCategory + " >");
        tvReminderValue.setText(reminderMinutes == 0 ? "Không Nhắc >" : "Trước " + reminderMinutes + " phút >");
    }

    private void setupEvents() {
        // Chọn ngày
        findViewById(R.id.row_date).setOnClickListener(v -> showDatePicker());

        // Chọn danh mục
        findViewById(R.id.row_category).setOnClickListener(v -> showCategoryMenu(v));

        // Chọn nhắc nhở
        findViewById(R.id.row_reminder).setOnClickListener(v -> showReminderDialog());

        View.OnClickListener saveAction = v -> saveEvent();
        btnSaveTop.setOnClickListener(saveAction);
        btnSaveBottom.setOnClickListener(saveAction);

        btnDelete.setOnClickListener(v -> deleteEvent());
    }

    private void showDatePicker() {
        CustomCalendarBottomSheet calendarSheet = new CustomCalendarBottomSheet(calendar.getTimeInMillis(), dateInMillis -> {
            calendar.setTimeInMillis(dateInMillis);
            updateUI();
        });
        calendarSheet.show(getSupportFragmentManager(), "CalendarSheet");
    }

    private void showCategoryMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Cuộc Sống");
        popup.getMenu().add("Công Việc");
        popup.getMenu().add("Sinh Nhật");
        popup.getMenu().add("Kỷ Niệm");

        popup.setOnMenuItemClickListener(item -> {
            selectedCategory = item.getTitle().toString();
            updateUI();
            return true;
        });
        popup.show();
    }

    private void showReminderDialog() {
        EditText edtMinutes = new EditText(this);
        edtMinutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        edtMinutes.setHint("Nhập số phút (VD: 15)");

        new AlertDialog.Builder(this)
                .setTitle("Nhắc nhở trước bao lâu?")
                .setView(edtMinutes)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String s = edtMinutes.getText().toString();
                    if (!s.isEmpty()) {
                        reminderMinutes = Integer.parseInt(s);
                        updateUI();
                    }
                })
                .setNegativeButton("Hủy", null).show();
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

        currentEvent.title = title;
        currentEvent.targetDate = calendar.getTimeInMillis();
        currentEvent.category = selectedCategory;
        currentEvent.reminderMinutes = reminderMinutes;

        SessionManager session = new SessionManager(this);
        currentEvent.userId = session.getUserId();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (currentEvent.id == 0) db.countdownDao().insert(currentEvent);
            else db.countdownDao().update(currentEvent);

            SyncHelper.autoBackup(this);

            runOnUiThread(() -> {
                Toast.makeText(this, "Đã lưu thành công!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void deleteEvent() {
        if (currentEvent == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            db.countdownDao().delete(currentEvent);
            SyncHelper.autoBackup(this);
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xóa sự kiện!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}