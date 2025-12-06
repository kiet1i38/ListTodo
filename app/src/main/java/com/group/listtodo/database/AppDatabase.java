package com.group.listtodo.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.models.Task;
import com.group.listtodo.models.TimerPreset;

// Tăng version lên 6
@Database(entities = {Task.class, CountdownEvent.class, TimerPreset.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TaskDao taskDao();
    public abstract CountdownDao countdownDao();
    public abstract TimerDao timerDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "list_todo_db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration() // <--- Dòng này giúp tự xóa DB cũ, tránh Crash
                    .build();
        }
        return instance;
    }
}