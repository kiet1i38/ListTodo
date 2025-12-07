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

    public static boolean isRunning = false;
    public static int currentTimerId = -1;
    public static long currentTimeLeft = 0;

    private CountDownTimer timer;
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        if ("STOP".equals(intent.getAction())) {
            stopTimer();
            return START_NOT_STICKY;
        }

        String title = intent.getStringExtra("TITLE");
        long duration = intent.getLongExtra("DURATION", 0);
        int timerId = intent.getIntExtra("TIMER_ID", -1);

        startForeground(NOTIFICATION_ID, createNotification(title, "Đang khởi động..."));

        if (timer != null) {
            timer.cancel();
        }

        currentTimerId = timerId;
        isRunning = true;

        timer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTimeLeft = millisUntilFinished;

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, createNotification(title, "Còn lại: " + formatTime(millisUntilFinished)));
                }

                Intent i = new Intent(ACTION_UPDATE);
                i.putExtra("TIME_LEFT", millisUntilFinished);
                i.putExtra("TIMER_ID", currentTimerId); 
                sendBroadcast(i);
            }

            @Override
            public void onFinish() {
                Intent i = new Intent(ACTION_FINISH);
                i.putExtra("TIMER_ID", currentTimerId);
                sendBroadcast(i);
                stopTimer();
            }
        }.start();

        return START_NOT_STICKY;
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isRunning = false;
        currentTimerId = -1; 
        stopForeground(true);
        stopSelf();
    }

    private Notification createNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, TimerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private String formatTime(long millis) {
        long h = (millis / 1000) / 3600;
        long m = ((millis / 1000) % 3600) / 60;
        long s = (millis / 1000) % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        stopTimer(); 
        super.onDestroy();
    }
}
