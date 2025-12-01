package com.group.listtodo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.group.listtodo.models.Task;
import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    void insertTask(Task task);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);

    // Lấy tất cả task của User (Cho màn hình chính)
    @Query("SELECT * FROM tasks WHERE userId = :uid ORDER BY dueDate ASC")
    List<Task> getAllTasks(String uid);

    // Lấy task theo Quadrant (SỬA LỖI: Bỏ điều kiện isCompleted = 0 để hiện cả việc đã xong)
    @Query("SELECT * FROM tasks WHERE userId = :uid AND priority = :prio ORDER BY dueDate ASC")
    List<Task> getTasksByPriority(String uid, int prio);
}