package com.group.listtodo.models;

import java.io.Serializable;

public class SubtaskItem implements Serializable {
    public String title;
    public boolean isCompleted;
    public long dueDate;      // Thêm: Ngày giờ
    public int priority;      // Thêm: Cấp bậc
    public String note;       // Thêm: Ghi chú
    // Em có thể thêm location, remind... tùy ý

    public SubtaskItem(String title, boolean isCompleted) {
        this.title = title;
        this.isCompleted = isCompleted;
        this.priority = 4; // Mặc định bình thường
        this.dueDate = System.currentTimeMillis();
        this.note = "";
    }
}