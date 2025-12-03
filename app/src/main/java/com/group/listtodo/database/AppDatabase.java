package com.group.listtodo.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.models.Task;
import com.group.listtodo.models.TimerPreset; // <--- Import mới

// 1. Thêm TimerPreset.class vào entities
// 2. Tăng version lên 4 (để reset DB)
@Database(entities = {Task.class, CountdownEvent.class, TimerPreset.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    // Các DAO cũ
    public abstract TaskDao taskDao();
    public abstract CountdownDao countdownDao();

    // --- THÊM DÒNG NÀY ĐỂ SỬA LỖI ---
    public abstract TimerDao timerDao();
    // --------------------------------

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "list_todo_db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration() // Tự động xóa DB cũ khi đổi version
                    .build();
        }
        return instance;
    }
}