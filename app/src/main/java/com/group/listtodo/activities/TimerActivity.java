package com.group.listtodo.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import com.group.listtodo.services.TimerService;
import java.util.Locale;

public class TimerActivity extends AppCompatActivity {

    private TextView tvTimer;
    private EditText edtMinutes;
    private Button btnStart, btnStop;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        // 1. Xử lý nút Back (QUAN TRỌNG: Để không bị kẹt ở màn hình này)
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 2. Ánh xạ View
        tvTimer = findViewById(R.id.tv_timer_display);
        edtMinutes = findViewById(R.id.edt_minutes);
        btnStart = findViewById(R.id.btn_start_timer);
        btnStop = findViewById(R.id.btn_stop_timer);

        // 3. Sự kiện Bắt đầu
        btnStart.setOnClickListener(v -> {
            String minStr = edtMinutes.getText().toString();
            if (minStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số phút!", Toast.LENGTH_SHORT).show();
                return;
            }

            long minutes = Long.parseLong(minStr);
            if (minutes <= 0) {
                Toast.makeText(this, "Số phút phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }

            long duration = minutes * 60 * 1000;

            // Khởi động Service chạy ngầm
            Intent intent = new Intent(this, TimerService.class);
            intent.putExtra("DURATION", duration);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            Toast.makeText(this, "Đã bắt đầu hẹn giờ!", Toast.LENGTH_SHORT).show();
        });

        // 4. Sự kiện Dừng lại
        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, TimerService.class));
            tvTimer.setText("00:00");
            Toast.makeText(this, "Đã hủy hẹn giờ", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Đăng ký nhận thông tin từ Service để cập nhật giao diện
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null) {
                    if (intent.getAction().equals(TimerService.ACTION_UPDATE)) {
                        long timeLeft = intent.getLongExtra("TIME_LEFT", 0);
                        int minutes = (int) (timeLeft / 1000) / 60;
                        int seconds = (int) (timeLeft / 1000) % 60;
                        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                    } else if (intent.getAction().equals(TimerService.ACTION_FINISH)) {
                        tvTimer.setText("Hết giờ!");
                        Toast.makeText(context, "Đã hết giờ!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerService.ACTION_UPDATE);
        filter.addAction(TimerService.ACTION_FINISH);

        // Android 13+ yêu cầu cờ RECEIVER_EXPORTED hoặc NOT_EXPORTED
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hủy đăng ký để tránh lỗi rò rỉ bộ nhớ (Memory Leak)
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                // Bỏ qua lỗi nếu chưa đăng ký
            }
        }
    }
}