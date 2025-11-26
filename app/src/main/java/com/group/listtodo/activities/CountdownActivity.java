package com.group.listtodo.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class CountdownActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        setupEvent(R.id.tv_days_tet, 1, 1); // Tết (1/1 Âm - Demo để 1/1 Dương)
        setupEvent(R.id.tv_days_xmas, 25, 12); // Giáng sinh
    }

    private void setupEvent(int textViewId, int day, int month) {
        TextView tv = findViewById(textViewId);
        Calendar today = Calendar.getInstance();
        Calendar event = Calendar.getInstance();
        event.set(Calendar.DAY_OF_MONTH, day);
        event.set(Calendar.MONTH, month - 1); // Month start 0

        if (event.before(today)) {
            event.add(Calendar.YEAR, 1);
        }

        long diff = event.getTimeInMillis() - today.getTimeInMillis();
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        tv.setText(days + " Ngày");
    }
}