package com.group.listtodo.models;

import java.io.Serializable;

public class SubtaskItem implements Serializable {
    public String title;
    public boolean isCompleted;
    public long dueDate;
    public int priority;
    public String note;
    public String location;

    // --- THÊM MỚI ---
    public int reminderMinutes; // 0 = Không nhắc
    public String soundName;    // Tên file nhạc
    // ----------------

    public SubtaskItem(String title, boolean isCompleted) {
        this.title = title;
        this.isCompleted = isCompleted;
        this.priority = 4;
        this.dueDate = System.currentTimeMillis();
        this.note = "";
        this.location = "";
        this.reminderMinutes = 0;
        this.soundName = "sound_alarm";
    }
}