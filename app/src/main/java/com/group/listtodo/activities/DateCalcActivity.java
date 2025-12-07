package com.group.listtodo.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateCalcActivity extends AppCompatActivity {

    // Phần 1: Tính Ngày
    private TextView tvCalcStartDate, tvResultBefore, tvResultAfter;
    private EditText edtDaysBefore, edtDaysAfter;
    private Calendar calCalculation = Calendar.getInstance();

    // Phần 2: Khoảng cách
    private TextView tvDiffStart, tvDiffEnd, tvDiffTotal, tvDiffDetail1, tvDiffDetail2;
    private Switch switchIncludeStart;
    private Calendar calDiffStart = Calendar.getInstance();
    private Calendar calDiffEnd = Calendar.getInstance();

    // Format ngày hiển thị (VD: 2025.11.27)
    private SimpleDateFormat sdfShort = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
    // Format ngày kết quả (VD: thg 11 27, 2025 Thứ Năm)
    private SimpleDateFormat sdfLong = new SimpleDateFormat("MMM dd, yyyy EEEE", new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_calc);

        initViews();
        setupEvents();

        // Hiển thị mặc định
        updateCalculationUI();
        updateDurationUI();
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Part 1
        tvCalcStartDate = findViewById(R.id.tv_calc_start_date);
        edtDaysBefore = findViewById(R.id.edt_days_before);
        edtDaysAfter = findViewById(R.id.edt_days_after);
        tvResultBefore = findViewById(R.id.tv_result_before);
        tvResultAfter = findViewById(R.id.tv_result_after);

        // Part 2
        tvDiffStart = findViewById(R.id.tv_diff_start);
        tvDiffEnd = findViewById(R.id.tv_diff_end);
        tvDiffTotal = findViewById(R.id.tv_diff_total);
        tvDiffDetail1 = findViewById(R.id.tv_diff_detail_1);
        tvDiffDetail2 = findViewById(R.id.tv_diff_detail_2);
        switchIncludeStart = findViewById(R.id.switch_include_start);
    }

    private void setupEvents() {
        // --- Phần 1: Sự kiện ---
        tvCalcStartDate.setOnClickListener(v -> showDatePicker(calCalculation, this::updateCalculationUI));

        TextWatcher textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateCalculationUI(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        edtDaysBefore.addTextChangedListener(textWatcher);
        edtDaysAfter.addTextChangedListener(textWatcher);

        // --- Phần 2: Sự kiện ---
        tvDiffStart.setOnClickListener(v -> showDatePicker(calDiffStart, this::updateDurationUI));
        tvDiffEnd.setOnClickListener(v -> showDatePicker(calDiffEnd, this::updateDurationUI));

        switchIncludeStart.setOnCheckedChangeListener((buttonView, isChecked) -> updateDurationUI());
    }

    // Thay thế hàm showDatePicker cũ bằng hàm này
    private void showDatePicker(Calendar calendarToSet, Runnable onDateSet) {
        CustomCalendarBottomSheet calendarSheet = new CustomCalendarBottomSheet(calendarToSet.getTimeInMillis(), dateInMillis -> {
            calendarToSet.setTimeInMillis(dateInMillis);
            onDateSet.run();
        });
        calendarSheet.show(getSupportFragmentManager(), "CalendarSheet");
    }

    // --- LOGIC PHẦN 1: TÍNH NGÀY TRƯỚC/SAU ---
    private void updateCalculationUI() {
        tvCalcStartDate.setText(sdfShort.format(calCalculation.getTime()));

        // Tính ngày TRƯỚC
        String strBefore = edtDaysBefore.getText().toString();
        int daysBefore = strBefore.isEmpty() ? 0 : Integer.parseInt(strBefore);
        Calendar calBefore = (Calendar) calCalculation.clone();
        calBefore.add(Calendar.DAY_OF_YEAR, -daysBefore);
        tvResultBefore.setText(sdfLong.format(calBefore.getTime()));

        // Tính ngày SAU
        String strAfter = edtDaysAfter.getText().toString();
        int daysAfter = strAfter.isEmpty() ? 0 : Integer.parseInt(strAfter);
        Calendar calAfter = (Calendar) calCalculation.clone();
        calAfter.add(Calendar.DAY_OF_YEAR, daysAfter);
        tvResultAfter.setText(sdfLong.format(calAfter.getTime()));
    }

    // --- LOGIC PHẦN 2: TÍNH KHOẢNG CÁCH ---
    private void updateDurationUI() {
        tvDiffStart.setText(sdfShort.format(calDiffStart.getTime()));
        tvDiffEnd.setText(sdfShort.format(calDiffEnd.getTime()));

        long diffInMillis = Math.abs(calDiffEnd.getTimeInMillis() - calDiffStart.getTimeInMillis());
        long totalDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        // Nếu bật switch bao gồm ngày bắt đầu -> Cộng thêm 1
        if (switchIncludeStart.isChecked()) {
            totalDays += 1;
        }

        tvDiffTotal.setText(String.valueOf(totalDays));

        // Tính chi tiết (Tháng/Tuần)
        long weeks = totalDays / 7;
        long daysLeftWeek = totalDays % 7;

        // Tính tháng (ước lượng 30 ngày cho đơn giản hoặc dùng Calendar chính xác)
        // Dùng Calendar để tính chính xác tháng
        int months = 0;
        int daysLeftMonth = 0;
        // Logic tính tháng chính xác khá phức tạp, ở đây dùng ước lượng 30.44 ngày hoặc hiển thị tuần cho đẹp
        // Theo hình mẫu: "4 Tuần 3 Ngày"

        tvDiffDetail1.setText(totalDays / 30 + " Tháng " + (totalDays % 30) + " Ngày");
        tvDiffDetail2.setText(weeks + " Tuần " + daysLeftWeek + " Ngày");
    }
}