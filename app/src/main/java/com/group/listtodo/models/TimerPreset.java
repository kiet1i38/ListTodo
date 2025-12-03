package com.group.listtodo.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "timers")
public class TimerPreset implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public long durationInMillis; // Tổng thời gian gốc (Ví dụ: 30 phút)
    public long remainingTime;    // Thời gian còn lại (Ví dụ: còn 15 phút) -> THÊM MỚI
    public int iconResId;
    public int colorResId;
    public String userId;

    public TimerPreset() {}

    public TimerPreset(String title, long durationInMillis, int iconResId, int colorResId) {
        this.title = title;
        this.durationInMillis = durationInMillis;
        this.remainingTime = durationInMillis; // Mặc định khi tạo mới thì còn lại = tổng
        this.iconResId = iconResId;
        this.colorResId = colorResId;
    }
}