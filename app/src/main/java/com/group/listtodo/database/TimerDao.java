package com.group.listtodo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update; // <--- Import thêm cái này
import com.group.listtodo.models.TimerPreset;
import java.util.List;

@Dao
public interface TimerDao {
    @Insert
    void insert(TimerPreset timer);

    // --- THÊM HÀM NÀY ĐỂ SỬA LỖI ---
    @Update
    void update(TimerPreset timer);
    // -------------------------------

    @Delete
    void delete(TimerPreset timer);

    @Query("SELECT * FROM timers WHERE userId = :uid")
    List<TimerPreset> getTimers(String uid);
}