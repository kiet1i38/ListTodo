package com.group.listtodo.models;

import java.util.List;

public class BackupData {
    public String userId;
    public List<Task> tasks;
    public List<TimerPreset> timers;
    public List<CountdownEvent> countdowns;

    // --- 1. CONSTRUCTOR RỖNG (BẮT BUỘC CHO FIREBASE) ---
    // Nếu thiếu cái này, App sẽ crash khi restore
    public BackupData() {
    }
    // ---------------------------------------------------

    // 2. Constructor đầy đủ (Dùng khi upload)
    public BackupData(String userId, List<Task> tasks, List<TimerPreset> timers, List<CountdownEvent> countdowns) {
        this.userId = userId;
        this.tasks = tasks;
        this.timers = timers;
        this.countdowns = countdowns;
    }
}