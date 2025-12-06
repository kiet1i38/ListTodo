package com.group.listtodo.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.TimerPreset;
import com.group.listtodo.services.TimerService;
import com.group.listtodo.utils.SyncHelper;

import java.util.Locale;
import java.util.concurrent.Executors;

public class TimerRunningActivity extends AppCompatActivity {

    private TextView tvTitle, tvCountdown;
    private FloatingActionButton btnPause, btnReset;
    private TimerPreset timer;
    private BroadcastReceiver receiver;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_running);

        db = AppDatabase.getInstance(this);
        timer = (TimerPreset) getIntent().getSerializableExtra("timer");
        if (timer == null) { finish(); return; }

        tvTitle = findViewById(R.id.tv_title);
        tvCountdown = findViewById(R.id.tv_countdown);
        btnPause = findViewById(R.id.btn_pause_resume);
        btnReset = findViewById(R.id.btn_reset);

        tvTitle.setText(timer.title);

        // --- CHECK LOGIC KHI VỪA MỞ ---
        // Chỉ hiển thị đang chạy nếu ID trùng khớp
        if (TimerService.isRunning && TimerService.currentTimerId == timer.id) {
            updateUI(true);
            updateTimerText(TimerService.currentTimeLeft);
        } else {
            updateUI(false);
            updateTimerText(timer.remainingTime > 0 ? timer.remainingTime : timer.durationInMillis);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // --- 1. SỬA LỖI NÚT XÓA ---
        findViewById(R.id.btn_delete_timer).setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                // Nếu đang chạy đúng timer này -> Dừng Service ngay
                if (TimerService.isRunning && TimerService.currentTimerId == timer.id) {
                    Intent intent = new Intent(this, TimerService.class);
                    intent.setAction("STOP");
                    startService(intent);
                }

                // Xóa trong DB
                db.timerDao().delete(timer);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show();
                    SyncHelper.autoBackup(this);
                    finish();
                });
            });
        });

        // --- 2. SỬA LỖI NÚT PLAY/PAUSE ---
        btnPause.setOnClickListener(v -> {
            if (TimerService.isRunning && TimerService.currentTimerId == timer.id) {
                // PAUSE
                long currentLeft = TimerService.currentTimeLeft;

                // Dừng Service
                Intent intent = new Intent(this, TimerService.class);
                intent.setAction("STOP");
                startService(intent);

                // Lưu DB
                timer.remainingTime = currentLeft;
                saveTimerToDb();

                updateUI(false);
                updateTimerText(currentLeft);
            } else {
                // RESUME
                long timeToRun = (timer.remainingTime > 0) ? timer.remainingTime : timer.durationInMillis;
                startTimerService(timeToRun);
                updateUI(true);
            }
        });

        // --- 3. SỬA LỖI NÚT RESET ---
        btnReset.setOnClickListener(v -> {
            // Dừng Service ngay lập tức nếu đang chạy
            if (TimerService.isRunning && TimerService.currentTimerId == timer.id) {
                Intent intent = new Intent(this, TimerService.class);
                intent.setAction("STOP");
                startService(intent);
            }

            // Reset thời gian về gốc
            timer.remainingTime = timer.durationInMillis;
            saveTimerToDb();

            // Cập nhật UI ngay
            updateUI(false);
            updateTimerText(timer.durationInMillis);
            Toast.makeText(this, "Đã đặt lại!", Toast.LENGTH_SHORT).show();
            SyncHelper.autoBackup(this);
        });
    }

    private void startTimerService(long duration) {
        Intent intent = new Intent(this, TimerService.class);
        intent.putExtra("DURATION", duration);
        intent.putExtra("TIMER_ID", timer.id);
        intent.putExtra("TITLE", timer.title);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void saveTimerToDb() {
        Executors.newSingleThreadExecutor().execute(() -> db.timerDao().update(timer));
    }

    private void updateUI(boolean isRunning) {
        if (isRunning) {
            btnPause.setImageResource(R.drawable.ic_menu); // Pause (Icon 2 gạch)
        } else {
            btnPause.setImageResource(R.drawable.ic_check_circle); // Play (Icon tam giác)
        }
    }

    private void updateTimerText(long millis) {
        long h = (millis / 1000) / 3600;
        long m = ((millis / 1000) % 3600) / 60;
        long s = (millis / 1000) % 60;
        tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (TimerService.ACTION_UPDATE.equals(intent.getAction())) {
                    int id = intent.getIntExtra("TIMER_ID", -1);
                    // CHỈ UPDATE NẾU ID TRÙNG KHỚP
                    if (id == timer.id) {
                        long timeLeft = intent.getLongExtra("TIME_LEFT", 0);
                        updateTimerText(timeLeft);
                        updateUI(true);
                    }
                } else if (TimerService.ACTION_FINISH.equals(intent.getAction())) {
                    int id = intent.getIntExtra("TIMER_ID", -1);
                    if (id == timer.id) {
                        tvCountdown.setText("00:00:00");
                        updateUI(false);
                        timer.remainingTime = 0;
                        saveTimerToDb();
                    }
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

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) unregisterReceiver(receiver);
    }
}