package com.group.listtodo.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.group.listtodo.api.RetrofitClient;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import java.util.List;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncHelper {

    // Hàm này sẽ được gọi tự động mỗi khi dữ liệu thay đổi
    public static void autoBackup(Context context) {
        SessionManager session = new SessionManager(context);
        String userId = session.getUserId();

        if (userId == null) return; // Chưa đăng nhập thì thôi

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Task> localTasks = db.taskDao().getAllTasks(userId);

            // Gọi API đẩy lên Server ngầm
            RetrofitClient.getService().syncTasks(localTasks).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d("AutoSync", "Đã tự động sao lưu lên Server!");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e("AutoSync", "Lỗi kết nối Server: " + t.getMessage());
                }
            });
        });
    }
}