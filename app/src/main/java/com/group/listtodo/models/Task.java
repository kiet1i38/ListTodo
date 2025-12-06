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

    public String location;
    public double locationLat;
    public double locationLng;

    // --- CÁC TRƯỜNG MỚI CHO BÁO THỨC ---
    public int reminderMinutes; // Báo trước bao nhiêu phút (0 = đúng giờ)
    public int repeatCount;     // Số lần lặp lại
    public String soundName;    // Tên file âm thanh (VD: "sound_alarm")

    public Task() {}

    public Task(String title, long dueDate, int priority, String category) {
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
        this.category = category;
        this.isCompleted = false;
        this.reminderMinutes = 0; // Mặc định đúng giờ
        this.repeatCount = 0;     // Không lặp
        this.soundName = "sound_alarm"; // Âm thanh mặc định
    }
}