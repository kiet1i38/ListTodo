package com.group.listtodo.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import com.group.listtodo.models.Task;

@Database(entities = {Task.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TaskDao taskDao();

    // Singleton Pattern để đảm bảo chỉ có 1 kết nối DB duy nhất
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "todo_tick_database")
                    .allowMainThreadQueries() // Tạm thời cho phép chạy trên Main Thread để test nhanh (Tuần sau sẽ sửa thành Background Thread)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
