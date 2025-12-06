package com.group.listtodo.api;

import com.group.listtodo.models.BackupData;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/api/sync")
    Call<Void> syncData(@Body BackupData data);

    @GET("/api/get_data")
    Call<BackupData> getData(@Query("userId") String userId);
}