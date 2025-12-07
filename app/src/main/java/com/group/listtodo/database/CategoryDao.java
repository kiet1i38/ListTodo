package com.group.listtodo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.group.listtodo.models.Category;
import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    void insert(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories WHERE userId = :uid")
    List<Category> getCategories(String uid);

    @Query("DELETE FROM categories WHERE userId = :uid")
    void deleteAllByUser(String uid);
}