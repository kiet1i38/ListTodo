package com.group.listtodo.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.TimerPreset;
import com.group.listtodo.services.TimerService;
import com.group.listtodo.utils.SessionManager;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimerActivity extends AppCompatActivity {

    private LinearLayout container;
    private AppDatabase db;
    private String userId;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getUserId();
        container = findViewById(R.id.container_timers);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.fab_add_timer).setOnClickListener(v -> startActivity(new Intent(this, AddTimerActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTimers(); // Load lại list mỗi khi quay lại
        registerTimerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) unregisterReceiver(receiver);
    }

    private void loadTimers() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<TimerPreset> list = db.timerDao().getTimers(userId);
            runOnUiThread(() -> {
                container.removeAllViews();
                for (TimerPreset timer : list) {
                    addTimerView(timer);
                }
            });
        });
    }

    private void addTimerView(TimerPreset timer) {
        View view = getLayoutInflater().inflate(R.layout.item_timer_preset, container, false);
        view.setTag(timer.id);

        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvDuration = view.findViewById(R.id.tv_duration);
        CardView cardIcon = view.findViewById(R.id.card_icon);
        ImageView btnPlay = view.findViewById(R.id.btn_play_item);

        tvTitle.setText(timer.title);
        cardIcon.setCardBackgroundColor(getResources().getColor(timer.colorResId != 0 ? timer.colorResId : R.color.blue_primary));

        // Logic hiển thị trạng thái
        boolean isThisRunning = TimerService.isRunning && TimerService.currentTimerId == timer.id;
        long timeToShow = isThisRunning ? TimerService.currentTimeLeft : (timer.remainingTime > 0 ? timer.remainingTime : timer.durationInMillis);

        updateTimeText(tvDuration, timeToShow);
        btnPlay.setImageResource(isThisRunning ? R.drawable.ic_menu : R.drawable.ic_check_circle);

        view.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimerRunningActivity.class);
            intent.putExtra("timer", timer);
            startActivity(intent);
        });

        btnPlay.setOnClickListener(v -> handlePlayPause(timer, btnPlay, tvDuration));

        container.addView(view);
    }

    private void handlePlayPause(TimerPreset timer, ImageView btnPlay, TextView tvDuration) {
        if (TimerService.isRunning && TimerService.currentTimerId == timer.id) {
            Intent intent = new Intent(this, TimerService.class);
            intent.setAction("STOP");
            startService(intent);

            timer.remainingTime = TimerService.currentTimeLeft;
            Executors.newSingleThreadExecutor().execute(() -> db.timerDao().update(timer));

            btnPlay.setImageResource(R.drawable.ic_check_circle);
            updateTimeText(tvDuration, TimerService.currentTimeLeft);
        } else {
            long timeToRun = (timer.remainingTime > 0) ? timer.remainingTime : timer.durationInMillis;

            Intent intent = new Intent(this, TimerService.class);
            intent.putExtra("DURATION", timeToRun);
            intent.putExtra("TIMER_ID", timer.id);
            intent.putExtra("TITLE", timer.title);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            loadTimers();
        }
    }

    private void registerTimerReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (TimerService.ACTION_UPDATE.equals(intent.getAction())) {
                    int runningId = intent.getIntExtra("TIMER_ID", -1);
                    long timeLeft = intent.getLongExtra("TIME_LEFT", 0);

                    View itemView = container.findViewWithTag(runningId);
                    if (itemView != null) {
                        TextView tvDuration = itemView.findViewById(R.id.tv_duration);
                        updateTimeText(tvDuration, timeLeft);

                        ImageView btnPlay = itemView.findViewById(R.id.btn_play_item);
                        btnPlay.setImageResource(R.drawable.ic_menu);
                    }
                } else if (TimerService.ACTION_FINISH.equals(intent.getAction())) {
                    loadTimers(); 
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerService.ACTION_UPDATE);
        filter.addAction(TimerService.ACTION_FINISH);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    private void updateTimeText(TextView tv, long millis) {
        long h = (millis / 1000) / 3600;
        long m = ((millis / 1000) % 3600) / 60;
        long s = (millis / 1000) % 60;
        tv.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
    }
}
