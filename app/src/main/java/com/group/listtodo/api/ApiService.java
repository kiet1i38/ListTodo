package com.group.listtodo.api;

import com.group.listtodo.models.Task;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query; // <--- Import thêm cái này

public interface ApiService {
    // Gửi lên (Giữ nguyên, vì userId nằm trong Body rồi)
    @POST("/api/sync")
    Call<Void> syncTasks(@Body List<Task> tasks);

    // Lấy về (SỬA: Thêm tham số userId vào URL)
    // Ví dụ nó sẽ thành: /api/get_tasks?userId=abc123xyz
    @GET("/api/get_tasks")
    Call<List<Task>> getTasks(@Query("userId") String userId);
}