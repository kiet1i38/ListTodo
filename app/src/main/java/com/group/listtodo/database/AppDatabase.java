package com.group.listtodo.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.group.listtodo.models.CountdownEvent; // <--- Phải có dòng này
import com.group.listtodo.models.Task;

// 1. Phải có đủ 2 entities: Task.class VÀ CountdownEvent.class
// 2. Version nên tăng lên 2
@Database(entities = {Task.class, CountdownEvent.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TaskDao taskDao();

    // 3. Phải có dòng này để code gọi được CountdownDao
    public abstract CountdownDao countdownDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "list_todo_db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration() // Dòng này giúp tự xóa DB cũ nếu lệch version
                    .build();
        }
        return instance;
    }
}