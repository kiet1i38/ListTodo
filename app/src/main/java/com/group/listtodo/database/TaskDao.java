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

    // Lấy tất cả task, sắp xếp theo ngày gần nhất
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    List<Task> getAllTasks();

    // Tìm kiếm task theo tên (Cho chức năng Search)
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :keyword || '%'")
    List<Task> searchTasks(String keyword);

    // Lấy task theo độ ưu tiên (Cho màn hình 4 Quadrant)
    // Chỉ lấy những task chưa hoàn thành
    @Query("SELECT * FROM tasks WHERE priority = :priority AND isCompleted = 0")
    List<Task> getTasksByPriority(int priority);
}
