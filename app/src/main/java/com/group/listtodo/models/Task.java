package com.group.listtodo.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "tasks")
public class Task implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public long dueDate;
    public int priority;
    public boolean isCompleted;
    public String category;
    public String userId;
    public String subtasks;

    // --- CÁC TRƯỜNG ĐỊA ĐIỂM ---
    public String location;    // Tên địa điểm (VD: 19 Nguyễn Hữu Thọ)
    public double locationLat; // Vĩ độ (VD: 10.7324)
    public double locationLng; // Kinh độ (VD: 106.6992)

    public Task() {}

    public Task(String title, long dueDate, int priority, String category) {
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
        this.category = category;
        this.isCompleted = false;
    }
}