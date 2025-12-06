package com.group.listtodo.utils;

import android.content.Context;
import android.util.Log;
import com.group.listtodo.api.RetrofitClient;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.BackupData;
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.models.Task;
import com.group.listtodo.models.TimerPreset;
import java.util.List;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncHelper {

    public static void autoBackup(Context context) {
        if (context == null) return;

        SessionManager session = new SessionManager(context);
        String userId = session.getUserId();

        if (userId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            // 1. Lấy toàn bộ dữ liệu của User
            List<Task> tasks = db.taskDao().getAllTasks(userId);
            List<TimerPreset> timers = db.timerDao().getTimers(userId);
            List<CountdownEvent> countdowns = db.countdownDao().getAllEvents(userId);

            // 2. Gói lại
            BackupData backupData = new BackupData(userId, tasks, timers, countdowns);

            // 3. Gửi lên Server
            RetrofitClient.getService().syncData(backupData).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d("AutoSync", "Backup FULL thành công!");
                    } else {
                        Log.e("AutoSync", "Server lỗi: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e("AutoSync", "Lỗi kết nối: " + t.getMessage());
                }
            });
        });
    }
}