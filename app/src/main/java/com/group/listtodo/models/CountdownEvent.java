package com.group.listtodo.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "countdowns")
public class CountdownEvent implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String userId;

    public String title;
    public long targetDate; // Ngày mục tiêu
    public String category; // "Cuộc sống", "Công việc"...
    public boolean isPinned; // Đính kèm lên trên
    public boolean hasEndDate; // Có thời gian kết thúc không
    public int reminderMinutes;
    public String soundName;

    // Constructor
    public CountdownEvent() {}

    public CountdownEvent(String title, long targetDate) {
        this.title = title;
        this.targetDate = targetDate;
        this.category = "Cuộc Sống";
        this.isPinned = false;
        this.reminderMinutes = 0;
        this.soundName = "sound_alarm";
    }
}