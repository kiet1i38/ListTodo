package com.group.listtodo.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateCalcActivity extends AppCompatActivity {

    // Phần 1: Cộng trừ ngày
    private Button btnCalcStart;
    private EditText edtDays;
    private Spinner spinnerMode;
    private TextView tvCalcResult;
    private Calendar c1Start = Calendar.getInstance();

    // Phần 2: Khoảng cách
    private Button btnDiffStart, btnDiffEnd;
    private TextView tvDiffResult;
    private Calendar c2Start = Calendar.getInstance();
    private Calendar c2End = Calendar.getInstance();

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_calc);

        initViews();
        setupEvents();

        // Hiển thị mặc định
        updateCalcUI();
        updateDiffUI();
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Part 1
        btnCalcStart = findViewById(R.id.btn_calc_start_date);
        edtDays = findViewById(R.id.edt_days_to_add);
        spinnerMode = findViewById(R.id.spinner_calc_mode);
        tvCalcResult = findViewById(R.id.tv_calc_result_date);

        // Part 2
        btnDiffStart = findViewById(R.id.btn_diff_start);
        btnDiffEnd = findViewById(R.id.btn_diff_end);
        tvDiffResult = findViewById(R.id.tv_diff_result);
    }

    private void setupEvents() {
        // --- Logic Phần 1 ---
        btnCalcStart.setOnClickListener(v -> showDatePicker(c1Start, () -> updateCalcUI()));

        edtDays.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateCalcUI(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { updateCalcUI(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- Logic Phần 2 ---
        btnDiffStart.setOnClickListener(v -> showDatePicker(c2Start, () -> updateDiffUI()));
        btnDiffEnd.setOnClickListener(v -> showDatePicker(c2End, () -> updateDiffUI()));
    }

    private void showDatePicker(Calendar calendarToSet, Runnable onDateSet) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendarToSet.set(year, month, dayOfMonth);
            onDateSet.run();
        }, calendarToSet.get(Calendar.YEAR), calendarToSet.get(Calendar.MONTH), calendarToSet.get(Calendar.DAY_OF_MONTH)).show();
    }

    // Logic tính cộng trừ ngày
    private void updateCalcUI() {
        btnCalcStart.setText(sdf.format(c1Start.getTime()));

        String daysStr = edtDays.getText().toString();
        int days = daysStr.isEmpty() ? 0 : Integer.parseInt(daysStr);
        boolean isAdd = spinnerMode.getSelectedItemPosition() == 0; // 0: Sau, 1: Trước

        Calendar resultCal = (Calendar) c1Start.clone();
        resultCal.add(Calendar.DAY_OF_YEAR, isAdd ? days : -days);

        SimpleDateFormat fullFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        tvCalcResult.setText(fullFormat.format(resultCal.getTime()));
    }

    // Logic tính khoảng cách
    private void updateDiffUI() {
        btnDiffStart.setText(sdf.format(c2Start.getTime()));
        btnDiffEnd.setText(sdf.format(c2End.getTime()));

        long diff = Math.abs(c2End.getTimeInMillis() - c2Start.getTimeInMillis());
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        tvDiffResult.setText(String.valueOf(days));
    }
}