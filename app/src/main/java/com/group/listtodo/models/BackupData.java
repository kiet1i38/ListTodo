package com.group.listtodo.models;

import java.util.List;

public class BackupData {
    public String userId;
    public List<Task> tasks;
    public List<TimerPreset> timers;
    public List<CountdownEvent> countdowns;

    public BackupData() {
    }

    public BackupData(String userId, List<Task> tasks, List<TimerPreset> timers, List<CountdownEvent> countdowns) {
        this.userId = userId;
        this.tasks = tasks;
        this.timers = timers;
        this.countdowns = countdowns;
    }
}
