package com.group.listtodo.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.group.listtodo.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CustomCalendarBottomSheet extends BottomSheetDialogFragment {

    private OnDateSelectedListener listener;
    private long initialDate;
    private TextView tvCurrentMonth; // <--- Biến mới

    public interface OnDateSelectedListener {
        void onDateSelected(long dateInMillis);
    }

    public CustomCalendarBottomSheet(long initialDate, OnDateSelectedListener listener) {
        this.initialDate = initialDate;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_calendar_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CalendarView calendarView = view.findViewById(R.id.calendar_view_custom);
        tvCurrentMonth = view.findViewById(R.id.tv_current_month); // <--- Ánh xạ
        ImageView btnPrev = view.findViewById(R.id.btn_prev_month);
        ImageView btnNext = view.findViewById(R.id.btn_next_month);

        if (initialDate != 0) {
            calendarView.setDate(initialDate);
            updateMonthTitle(initialDate); // <--- Cập nhật lúc mở
        } else {
            updateMonthTitle(System.currentTimeMillis());
        }

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth);
            if (listener != null) {
                listener.onDateSelected(c.getTimeInMillis());
            }
            dismiss();
        });

        btnPrev.setOnClickListener(v -> changeMonth(calendarView, -1));
        btnNext.setOnClickListener(v -> changeMonth(calendarView, 1));
    }

    private void changeMonth(CalendarView calendarView, int amount) {
        long currentDateMillis = calendarView.getDate();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentDateMillis);
        c.add(Calendar.MONTH, amount);

        long newTime = c.getTimeInMillis();
        calendarView.setDate(newTime, true, true);
        updateMonthTitle(newTime); // <--- Cập nhật khi bấm nút
    }

    private void updateMonthTitle(long timeInMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeInMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
        tvCurrentMonth.setText(sdf.format(c.getTime()));
    }
}