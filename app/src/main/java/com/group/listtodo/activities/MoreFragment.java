package com.group.listtodo.activities;

import android.app.AlertDialog;
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
import com.group.listtodo.models.BackupData;
import com.group.listtodo.models.CountdownEvent;
import com.group.listtodo.models.Task;
import com.group.listtodo.models.TimerPreset;
import com.group.listtodo.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;

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
        setupItem(view.findViewById(R.id.menu_timer), "Đồng Hồ Hẹn Giờ", R.drawable.ic_clock, v -> {
            startActivity(new Intent(getContext(), TimerActivity.class));
        });

        // 4. Máy Tính Ngày Tháng
        setupItem(view.findViewById(R.id.menu_date_calc), "Máy Tính Ngày Tháng", R.drawable.ic_calculate, v -> {
            startActivity(new Intent(getContext(), DateCalcActivity.class));
        });

        // 5. Thống Kê
        setupItem(view.findViewById(R.id.menu_stats), "Thống Kê Todoits", R.drawable.ic_dashboard, v -> {
            startActivity(new Intent(getContext(), StatsActivity.class));
        });

        // 6. Đồng Bộ Server (Upload)
        // Tìm view theo ID trong fragment_more.xml
        View menuUpload = view.findViewById(R.id.menu_sync_upload);
        if (menuUpload != null) {
            setupItem(menuUpload, "Sao Lưu Lên Cloud (Upload)", R.drawable.ic_check_circle, v -> {
                syncDataToServer();
            });
        }

        // 7. Khôi Phục Dữ Liệu (Download)
        View menuDownload = view.findViewById(R.id.menu_sync_download);
        if (menuDownload != null) {
            setupItem(menuDownload, "Khôi Phục Dữ Liệu (Download)", R.drawable.ic_menu, v -> {
                showRestoreConfirmation();
            });
        }

        // 8. Đăng Xuất
        View btnLogout = view.findViewById(R.id.menu_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // Đăng xuất Firebase
                FirebaseAuth.getInstance().signOut();
                // Xóa Session
                new SessionManager(getContext()).logout();

                // Quay về màn hình Login
                Intent intent = new Intent(getContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
    }

    // --- LOGIC SAO LƯU (UPLOAD FULL) ---
    private void syncDataToServer() {
        Toast.makeText(getContext(), "Đang sao lưu...", Toast.LENGTH_SHORT).show();
        SessionManager session = new SessionManager(getContext());
        String userId = session.getUserId();

        if (userId == null) {
            Toast.makeText(getContext(), "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());

            // 1. Lấy TOÀN BỘ dữ liệu của User này
            List<Task> tasks = db.taskDao().getAllTasks(userId);
            List<TimerPreset> timers = db.timerDao().getTimers(userId);
            List<CountdownEvent> countdowns = db.countdownDao().getAllEvents(userId);

            // 2. Đóng gói vào BackupData
            BackupData data = new BackupData(userId, tasks, timers, countdowns);

            // 3. Gửi lên Server thông qua API syncData
            RetrofitClient.getService().syncData(data).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Sao lưu thành công!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(getContext(), "Lỗi kết nối: Hãy bật Server Python!", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // --- LOGIC KHÔI PHỤC (DOWNLOAD FULL) ---
    private void showRestoreConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Khôi Phục Dữ Liệu")
                .setMessage("Hành động này sẽ tải dữ liệu từ Server về và thay thế dữ liệu hiện tại trên máy. Bạn có chắc chắn?")
                .setPositiveButton("Khôi Phục", (dialog, which) -> restoreDataFromServer())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void restoreDataFromServer() {
        Toast.makeText(getContext(), "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show();
        SessionManager session = new SessionManager(getContext());
        String currentUserId = session.getUserId();

        if (currentUserId == null) return;

        // Gọi API lấy dữ liệu về
        RetrofitClient.getService().getData(currentUserId).enqueue(new Callback<BackupData>() {
            @Override
            public void onResponse(Call<BackupData> call, Response<BackupData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BackupData data = response.body();

                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(getContext());

                        // 1. Xóa sạch dữ liệu cũ của user này trên máy
                        db.taskDao().deleteAllByUser(currentUserId);
                        db.timerDao().deleteAllByUser(currentUserId);
                        db.countdownDao().deleteAllByUser(currentUserId);

                        // 2. Nạp dữ liệu mới từ Server vào
                        int countTask = 0;
                        int countTimer = 0;
                        int countEvent = 0;

                        if (data.tasks != null) {
                            for (Task t : data.tasks) {
                                t.id = 0; // Reset ID để tạo mới
                                db.taskDao().insertTask(t);
                                countTask++;
                            }
                        }
                        if (data.timers != null) {
                            for (TimerPreset t : data.timers) {
                                t.id = 0;
                                db.timerDao().insert(t);
                                countTimer++;
                            }
                        }
                        if (data.countdowns != null) {
                            for (CountdownEvent c : data.countdowns) {
                                c.id = 0;
                                db.countdownDao().insert(c);
                                countEvent++;
                            }
                        }

                        String msg = String.format("Đã khôi phục: %d việc, %d hẹn giờ, %d sự kiện.", countTask, countTimer, countEvent);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show()
                            );
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Không tìm thấy bản sao lưu nào!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BackupData> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper setup item menu
    private void setupItem(View itemView, String title, int iconRes, View.OnClickListener listener) {
        if (itemView == null) return;
        ImageView imgIcon = itemView.findViewById(R.id.img_icon);
        TextView tvTitle = itemView.findViewById(R.id.tv_title);

        tvTitle.setText(title);
        imgIcon.setImageResource(iconRes);

        itemView.setOnClickListener(listener);
    }
}