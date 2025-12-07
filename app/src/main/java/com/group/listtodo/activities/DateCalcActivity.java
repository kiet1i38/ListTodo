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

    private TextView tvCalcStartDate, tvResultBefore, tvResultAfter;
    private EditText edtDaysBefore, edtDaysAfter;
    private Calendar calCalculation = Calendar.getInstance();

    private TextView tvDiffStart, tvDiffEnd, tvDiffTotal, tvDiffDetail1, tvDiffDetail2;
    private Switch switchIncludeStart;
    private Calendar calDiffStart = Calendar.getInstance();
    private Calendar calDiffEnd = Calendar.getInstance();

    private SimpleDateFormat sdfShort = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
    private SimpleDateFormat sdfLong = new SimpleDateFormat("MMM dd, yyyy EEEE", new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_calc);

        initViews();
        setupEvents();

        updateCalculationUI();
        updateDurationUI();
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvCalcStartDate = findViewById(R.id.tv_calc_start_date);
        edtDaysBefore = findViewById(R.id.edt_days_before);
        edtDaysAfter = findViewById(R.id.edt_days_after);
        tvResultBefore = findViewById(R.id.tv_result_before);
        tvResultAfter = findViewById(R.id.tv_result_after);

        tvDiffStart = findViewById(R.id.tv_diff_start);
        tvDiffEnd = findViewById(R.id.tv_diff_end);
        tvDiffTotal = findViewById(R.id.tv_diff_total);
        tvDiffDetail1 = findViewById(R.id.tv_diff_detail_1);
        tvDiffDetail2 = findViewById(R.id.tv_diff_detail_2);
        switchIncludeStart = findViewById(R.id.switch_include_start);
    }

    private void setupEvents() {
        tvCalcStartDate.setOnClickListener(v -> showDatePicker(calCalculation, this::updateCalculationUI));

        TextWatcher textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateCalculationUI(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        edtDaysBefore.addTextChangedListener(textWatcher);
        edtDaysAfter.addTextChangedListener(textWatcher);

        tvDiffStart.setOnClickListener(v -> showDatePicker(calDiffStart, this::updateDurationUI));
        tvDiffEnd.setOnClickListener(v -> showDatePicker(calDiffEnd, this::updateDurationUI));

        switchIncludeStart.setOnCheckedChangeListener((buttonView, isChecked) -> updateDurationUI());
    }

    private void showDatePicker(Calendar calendarToSet, Runnable onDateSet) {
        CustomCalendarBottomSheet calendarSheet = new CustomCalendarBottomSheet(calendarToSet.getTimeInMillis(), dateInMillis -> {
            calendarToSet.setTimeInMillis(dateInMillis);
            onDateSet.run();
        });
        calendarSheet.show(getSupportFragmentManager(), "CalendarSheet");
    }

    private void updateCalculationUI() {
        tvCalcStartDate.setText(sdfShort.format(calCalculation.getTime()));

        String strBefore = edtDaysBefore.getText().toString();
        int daysBefore = strBefore.isEmpty() ? 0 : Integer.parseInt(strBefore);
        Calendar calBefore = (Calendar) calCalculation.clone();
        calBefore.add(Calendar.DAY_OF_YEAR, -daysBefore);
        tvResultBefore.setText(sdfLong.format(calBefore.getTime()));

        String strAfter = edtDaysAfter.getText().toString();
        int daysAfter = strAfter.isEmpty() ? 0 : Integer.parseInt(strAfter);
        Calendar calAfter = (Calendar) calCalculation.clone();
        calAfter.add(Calendar.DAY_OF_YEAR, daysAfter);
        tvResultAfter.setText(sdfLong.format(calAfter.getTime()));
    }

    private void updateDurationUI() {
        tvDiffStart.setText(sdfShort.format(calDiffStart.getTime()));
        tvDiffEnd.setText(sdfShort.format(calDiffEnd.getTime()));

        long diffInMillis = Math.abs(calDiffEnd.getTimeInMillis() - calDiffStart.getTimeInMillis());
        long totalDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        if (switchIncludeStart.isChecked()) {
            totalDays += 1;
        }

        tvDiffTotal.setText(String.valueOf(totalDays));

        long weeks = totalDays / 7;
        long daysLeftWeek = totalDays % 7;

        int months = 0;
        int daysLeftMonth = 0;

        tvDiffDetail1.setText(totalDays / 30 + " Tháng " + (totalDays % 30) + " Ngày");
        tvDiffDetail2.setText(weeks + " Tuần " + daysLeftWeek + " Ngày");
    }
}
