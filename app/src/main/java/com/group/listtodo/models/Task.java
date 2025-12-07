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

    public int reminderMinutes; 
    public int repeatCount;    
    public String soundName;   

    public Task() {}

    public Task(String title, long dueDate, int priority, String category) {
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
        this.category = category;
        this.isCompleted = false;
        this.reminderMinutes = 0; 
        this.repeatCount = 0;    
        this.soundName = "sound_alarm"; 
    }
}
