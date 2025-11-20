package com.group.listtodo.models;


import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "tasks")
public class Task implements Serializable {
    // Serializable giúp truyền object qua Intent giữa các Activity

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;         // Tên công việc
    public String description;   // Ghi chú thêm

    public long dueDate;         // Ngày hết hạn (Lưu dưới dạng timestamp - milliseconds)
    public boolean isCompleted;  // Trạng thái: Đã xong hay chưa

    // Mức độ ưu tiên cho 4 Quadrant:
    // 1: Quan trọng & Khẩn cấp (Đỏ)
    // 2: Quan trọng nhưng ko Khẩn (Vàng)
    // 3: Ko Quan trọng nhưng Khẩn (Xanh dương)
    // 4: Ko Quan trọng & ko Khẩn (Lục)
    public int priority;

    public String category;      // Ví dụ: "Công việc", "Cá nhân", "Gia đình"

    // Dành cho tính năng Novelty (Google Maps) sau này
    public double latitude;
    public double longitude;
    public String locationName;

    // Constructor rỗng (Bắt buộc cho Room)
    public Task() {
    }

    // Constructor tiện lợi để tạo nhanh
    public Task(String title, long dueDate, int priority, String category) {
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
        this.category = category;
        this.isCompleted = false;
    }
}