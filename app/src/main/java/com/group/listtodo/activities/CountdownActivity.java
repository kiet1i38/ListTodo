package com.group.listtodo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.utils.SessionManager; // Import
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CountdownActivity extends AppCompatActivity {

    private LinearLayout containerLayout;
    private AppDatabase db;
    private String userId; // Biến UserID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        db = AppDatabase.getInstance(this);

        // Lấy UserID
        SessionManager session = new SessionManager(this);
        userId = session.getUserId();

        containerLayout = findViewById(R.id.container_events);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.fab_add_event).setOnClickListener(v -> {
            startActivity(new Intent(this, AddCountdownActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // Lọc theo UserID
            List<CountdownEvent> events = db.countdownDao().getAllEvents(userId);

            runOnUiThread(() -> {
                if (containerLayout.getChildCount() > 0) {
                    containerLayout.removeAllViews();
                }
                for (CountdownEvent event : events) {
                    addEventView(event);
                }
            });
        });
    }

    private void addEventView(CountdownEvent event) {
        View itemView = getLayoutInflater().inflate(R.layout.item_countdown, containerLayout, false);

        TextView tvIconChar = itemView.findViewById(R.id.tv_icon_char);
        TextView tvTitle = itemView.findViewById(R.id.tv_event_title);
        TextView tvDate = itemView.findViewById(R.id.tv_target_date);
        TextView tvDays = itemView.findViewById(R.id.tv_days_remaining);
        TextView tvLabel = itemView.findViewById(R.id.tv_label_days);
        CardView cardIconBg = itemView.findViewById(R.id.card_icon_bg);

        tvTitle.setText(event.title);

        if (event.title != null && !event.title.isEmpty()) {
            tvIconChar.setText(String.valueOf(event.title.charAt(0)).toUpperCase());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDate.setText("Mục tiêu: " + sdf.format(new Date(event.targetDate)));

        long diff = event.targetDate - System.currentTimeMillis();
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (days >= 0) {
            tvDays.setText(String.valueOf(days));
            tvLabel.setText(" Ngày");
            tvDays.setTextColor(getResources().getColor(R.color.blue_primary));
            cardIconBg.setCardBackgroundColor(getResources().getColor(R.color.blue_primary));
        } else {
            tvDays.setText(String.valueOf(Math.abs(days)));
            tvLabel.setText(" Ngày qua");
            tvLabel.setTextColor(getResources().getColor(R.color.quadrant_2_orange));
            tvDays.setTextColor(getResources().getColor(R.color.quadrant_2_orange));
            cardIconBg.setCardBackgroundColor(getResources().getColor(R.color.quadrant_2_orange));
        }

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCountdownActivity.class);
            intent.putExtra("event", event);
            startActivity(intent);
        });

        containerLayout.addView(itemView);
    }
}