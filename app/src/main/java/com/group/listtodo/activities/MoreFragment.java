package com.group.listtodo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.group.listtodo.R;
import com.group.listtodo.api.RetrofitClient;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.Task;
import java.util.List;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Tiện ích Widget
        setupItem(view.findViewById(R.id.menu_widget), "Tiện Ích Máy Tính Để Bàn", R.drawable.ic_dashboard, v -> {
            Toast.makeText(getContext(), "Tính năng Widget đang phát triển", Toast.LENGTH_SHORT).show();
        });

        // 2. Ngày Đếm Ngược
        setupItem(view.findViewById(R.id.menu_countdown), "Ngày Đếm Ngược", R.drawable.ic_calendar, v -> {
            startActivity(new Intent(getContext(), CountdownActivity.class));
        });

        // 3. Đồng Hồ Hẹn Giờ
        setupItem(view.findViewById(R.id.menu_timer), "Đồng Hồ Hẹn Giờ", R.drawable.ic_check_circle, v -> {
            startActivity(new Intent(getContext(), TimerActivity.class));
        });

        // 4. Máy Tính Ngày Tháng
        setupItem(view.findViewById(R.id.menu_date_calc), "Máy Tính Ngày Tháng", R.drawable.ic_calendar, v -> {
            startActivity(new Intent(getContext(), DateCalcActivity.class));
        });

        // 5. Thống Kê
        setupItem(view.findViewById(R.id.menu_stats), "Thống Kê Todoits", R.drawable.ic_dashboard, v -> {
            startActivity(new Intent(getContext(), StatsActivity.class));
        });

        // 6. Đồng Bộ Server (Backend Flask)
        // Lưu ý: Đảm bảo em đã thêm CardView có id menu_sync trong file fragment_more.xml
        View menuSync = view.findViewById(R.id.menu_sync);
        if (menuSync != null) {
            setupItem(menuSync, "Đồng Bộ Đám Mây (Backup)", R.drawable.ic_check_circle, v -> {
                syncDataToServer();
            });
        }
    }

    // Hàm xử lý Logic Sync lên Server
    private void syncDataToServer() {
        Toast.makeText(getContext(), "Đang kết nối Server...", Toast.LENGTH_SHORT).show();

        // Chạy luồng phụ để lấy dữ liệu từ DB
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            List<Task> localTasks = db.taskDao().getAllTasks();

            if (localTasks.isEmpty()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Không có dữ liệu để backup!", Toast.LENGTH_SHORT).show()
                    );
                }
                return;
            }

            // Gọi API qua Retrofit
            RetrofitClient.getService().syncTasks(localTasks).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Backup thành công: " + localTasks.size() + " tasks", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    // Lỗi mạng hoặc Server chưa bật
                    Toast.makeText(getContext(), "Lỗi kết nối: Hãy bật Server Python!", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // Hàm hỗ trợ setup nhanh gọn
    private void setupItem(View itemView, String title, int iconRes, View.OnClickListener listener) {
        ImageView imgIcon = itemView.findViewById(R.id.img_icon);
        TextView tvTitle = itemView.findViewById(R.id.tv_title);

        tvTitle.setText(title);
        imgIcon.setImageResource(iconRes);

        itemView.setOnClickListener(listener);
    }
}