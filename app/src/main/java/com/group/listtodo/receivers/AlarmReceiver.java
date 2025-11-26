package com.group.listtodo.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.group.listtodo.R;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("TITLE");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "TaskReminder";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Task Reminder", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle("Nhắc nhở công việc")
                .setContentText("Đến hạn: " + taskTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}