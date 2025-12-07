package com.group.listtodo.receivers;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.group.listtodo.R;
import com.group.listtodo.services.AlarmSoundService;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("TITLE");
        String soundName = intent.getStringExtra("SOUND");
        int repeatCount = intent.getIntExtra("REPEAT", 0);
        int taskId = intent.getIntExtra("ID", 0);

        showNotification(context, taskTitle);

        Intent serviceIntent = new Intent(context, AlarmSoundService.class);
        serviceIntent.putExtra("SOUND_NAME", soundName);
        context.startService(serviceIntent);

        if (repeatCount > 0) {
            scheduleNextAlarm(context, taskId, taskTitle, soundName, repeatCount - 1);
        }
    }

    private void showNotification(Context context, String title) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "TaskReminder";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Task Reminder", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle("Đến giờ: " + title)
                .setContentText("Đã đến giờ làm việc!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void scheduleNextAlarm(Context context, int id, String title, String sound, int remainingRepeat) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);
        i.putExtra("TITLE", title);
        i.putExtra("SOUND", sound);
        i.putExtra("REPEAT", remainingRepeat);
        i.putExtra("ID", id);

        PendingIntent pi = PendingIntent.getBroadcast(context, id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        long nextTime = System.currentTimeMillis() + (5 * 60 * 1000);

        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pi);
        }
    }
}
