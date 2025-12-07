package com.group.listtodo.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "categories")
public class Category implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String userId;

    public Category() {}

    public Category(String name, String userId) {
        this.name = name;
        this.userId = userId;
    }
}