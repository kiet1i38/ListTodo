package com.group.listtodo.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.CountdownEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddCountdownActivity extends AppCompatActivity {

    private EditText edtTitle;
    private TextView tvDateValue, tvScreenTitle;
    private Switch switchPin;
    private Button btnSaveTop, btnSaveBottom, btnDelete;

    private Calendar calendar = Calendar.getInstance();
    private AppDatabase db;
    private CountdownEvent currentEvent; // Sự kiện đang sửa (nếu có)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_countdown);

        db = AppDatabase.getInstance(this);

        // Lấy dữ liệu truyền sang (nếu bấm sửa)
        currentEvent = (CountdownEvent) getIntent().getSerializableExtra("event");

        initViews();
        setupData();
        setupEvents();
    }

    private void initViews() {
        edtTitle = findViewById(R.id.edt_title);
        tvDateValue = findViewById(R.id.tv_date_value);
        tvScreenTitle = findViewById(R.id.tv_screen_title);
        switchPin = findViewById(R.id.switch_pin);
        btnSaveTop = findViewById(R.id.btn_save_top);
        btnSaveBottom = findViewById(R.id.btn_save_bottom);
        btnDelete = findViewById(R.id.btn_delete);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupData() {
        if (currentEvent != null) {
            // CHẾ ĐỘ SỬA
            tvScreenTitle.setText("Chỉnh Sửa Sự Kiện");
            edtTitle.setText(currentEvent.title);
            calendar.setTimeInMillis(currentEvent.targetDate);
            switchPin.setChecked(currentEvent.isPinned);
            btnDelete.setVisibility(View.VISIBLE); // Hiện nút xóa
        } else {
            // CHẾ ĐỘ THÊM MỚI
            tvScreenTitle.setText("Thêm Sự Kiện Nhỏ");
            btnDelete.setVisibility(View.GONE);
        }
        updateDateText();
    }

    private void setupEvents() {
        // Chọn ngày
        findViewById(R.id.row_date).setOnClickListener(v -> showDatePicker());

        // Lưu (Cả 2 nút trên và dưới đều gọi hàm save)
        View.OnClickListener saveAction = v -> saveEvent();
        btnSaveTop.setOnClickListener(saveAction);
        btnSaveBottom.setOnClickListener(saveAction);

        // Xóa
        btnDelete.setOnClickListener(v -> deleteEvent());
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            updateDateText();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
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
            currentEvent = new CountdownEvent(); // Tạo mới
        }

        // Update thông tin
        currentEvent.title = title;
        currentEvent.targetDate = calendar.getTimeInMillis();
        currentEvent.isPinned = switchPin.isChecked();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (currentEvent.id == 0) {
                db.countdownDao().insert(currentEvent); // Insert nếu id = 0
            } else {
                db.countdownDao().update(currentEvent); // Update nếu đã có id
            }

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
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xóa sự kiện!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}