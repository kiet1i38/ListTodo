package com.group.listtodo.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.group.listtodo.R;
import com.group.listtodo.activities.TimerActivity;

public class TimerService extends Service {
    public static final String CHANNEL_ID = "TimerChannel";
    public static final String ACTION_UPDATE = "com.group.listtodo.TIMER_UPDATE";
    public static final String ACTION_FINISH = "com.group.listtodo.TIMER_FINISH";

    private CountDownTimer timer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long duration = intent.getLongExtra("DURATION", 0);
        createNotificationChannel();

        // Tạo Notification chạy ngầm
        Intent notificationIntent = new Intent(this, TimerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TodoTick Timer")
                .setContentText("Đang đếm ngược...")
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // Bắt đầu đếm
        timer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Gửi broadcast để cập nhật UI
                Intent i = new Intent(ACTION_UPDATE);
                i.putExtra("TIME_LEFT", millisUntilFinished);
                sendBroadcast(i);
            }

            @Override
            public void onFinish() {
                Intent i = new Intent(ACTION_FINISH);
                sendBroadcast(i);
                stopSelf();
            }
        }.start();

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Timer Service Channel", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}