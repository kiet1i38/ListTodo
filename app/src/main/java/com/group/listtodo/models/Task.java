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
    public String category; // "Công Việc", "Cá Nhân"...
    public String userId;

    // THÊM MỚI: Lưu danh sách việc con (JSON hoặc String)
    // Format đơn giản: "Tên việc con 1,false;Tên việc con 2,true"
    public String subtasks;
    public String location; // Lưu tên địa điểm

    public Task() {}

    public Task(String title, long dueDate, int priority, String category) {
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
        this.category = category;
        this.isCompleted = false;
    }
}