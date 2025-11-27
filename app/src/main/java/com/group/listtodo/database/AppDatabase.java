package com.group.listtodo.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.models.Task;

// Lưu ý: Tăng version lên mỗi khi em sửa Model (thêm cột, thêm bảng)
@Database(entities = {Task.class, CountdownEvent.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TaskDao taskDao();
    public abstract CountdownDao countdownDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "list_todo_db")
                    .allowMainThreadQueries()
                    // DÒNG NÀY QUAN TRỌNG ĐỂ KHÔNG BỊ CRASH KHI SỬA DB:
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}