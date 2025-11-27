package com.group.listtodo.api;

import com.group.listtodo.models.Task;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    // Gửi list task lên server
    @POST("/api/sync")
    Call<Void> syncTasks(@Body List<Task> tasks);

    // Lấy list task về
    @GET("/api/get_tasks")
    Call<List<Task>> getTasks();
}