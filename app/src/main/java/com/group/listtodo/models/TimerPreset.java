package com.group.listtodo.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "timers")
public class TimerPreset implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public long durationInMillis;
    public long remainingTime;   
    public int iconResId;
    public int colorResId;
    public String userId;

    public TimerPreset() {}

    public TimerPreset(String title, long durationInMillis, int iconResId, int colorResId) {
        this.title = title;
        this.durationInMillis = durationInMillis;
        this.remainingTime = durationInMillis; 
        this.iconResId = iconResId;
        this.colorResId = colorResId;
    }
}
