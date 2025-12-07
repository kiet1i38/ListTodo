package com.group.listtodo.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.group.listtodo.models.Category;       
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.models.Task;
import com.group.listtodo.models.TimerPreset;

@Database(entities = {Task.class, CountdownEvent.class, TimerPreset.class, Category.class}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TaskDao taskDao();
    public abstract CountdownDao countdownDao();
    public abstract TimerDao timerDao();

    public abstract CategoryDao categoryDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "list_todo_db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration() 
                    .build();
        }
        return instance;
    }
}
