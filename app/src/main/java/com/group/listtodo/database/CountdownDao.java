package com.group.listtodo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.group.listtodo.models.CountdownEvent;
import java.util.List;

@Dao
public interface CountdownDao {
    @Insert
    void insert(CountdownEvent event);

    @Update
    void update(CountdownEvent event);

    @Delete
    void delete(CountdownEvent event);

    @Query("SELECT * FROM countdowns WHERE userId = :uid ORDER BY targetDate ASC")
    List<CountdownEvent> getAllEvents(String uid);

    // Thêm vào interface
    @Query("DELETE FROM countdowns WHERE userId = :uid")
    void deleteAllByUser(String uid);
}