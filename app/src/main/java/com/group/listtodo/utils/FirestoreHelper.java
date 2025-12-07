package com.group.listtodo.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.BackupData;
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.models.Task;
import com.group.listtodo.models.TimerPreset;
import java.util.List;
import java.util.concurrent.Executors;

public class FirestoreHelper {

    private static final String COLLECTION_BACKUPS = "backups";
    private static final String TAG = "FirestoreHelper";

    public static void backupToCloud(Context context) {
        SessionManager session = new SessionManager(context);
        String userId = session.getUserId();
        if (userId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            List<Task> tasks = db.taskDao().getAllTasks(userId);
            List<TimerPreset> timers = db.timerDao().getTimers(userId);
            List<CountdownEvent> countdowns = db.countdownDao().getAllEvents(userId);

            BackupData data = new BackupData(userId, tasks, timers, countdowns);

            FirebaseFirestore.getInstance()
                    .collection(COLLECTION_BACKUPS)
                    .document(userId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Backup lên Firebase thành công!"))
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi backup Firebase: " + e.getMessage()));
        });
    }

    public static void restoreFromCloud(Context context, String userId, Runnable onSuccess) {
        if (userId == null) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_BACKUPS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        BackupData data = documentSnapshot.toObject(BackupData.class);

                        if (data != null) {
                            saveToLocalDb(context, data, onSuccess);
                        } else {
                            if (onSuccess != null) onSuccess.run();
                        }
                    } else {
                        Log.d(TAG, "Chưa có dữ liệu trên Firebase.");
                        if (onSuccess != null) onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải dữ liệu Firebase: " + e.getMessage());
                    if (onSuccess != null) onSuccess.run();
                });
    }

    private static void saveToLocalDb(Context context, BackupData data, Runnable onSuccess) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            String uid = data.userId;

            db.taskDao().deleteAllByUser(uid);
            db.timerDao().deleteAllByUser(uid);
            db.countdownDao().deleteAllByUser(uid);

            if (data.tasks != null) {
                for (Task t : data.tasks) { t.id = 0; t.userId = uid; db.taskDao().insertTask(t); }
            }
            if (data.timers != null) {
                for (TimerPreset t : data.timers) { t.id = 0; t.userId = uid; db.timerDao().insert(t); }
            }
            if (data.countdowns != null) {
                for (CountdownEvent c : data.countdowns) { c.id = 0; c.userId = uid; db.countdownDao().insert(c); }
            }

            if (onSuccess != null) onSuccess.run();
        });
    }
}
