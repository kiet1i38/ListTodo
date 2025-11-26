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
             startActivity(new Intent(getContext(), CountdownActivity.class)); // Tuần sau mở
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
    }

    // Hàm hỗ trợ setup nhanh gọn
    private void setupItem(View itemView, String title, int iconRes, View.OnClickListener listener) {
        ImageView imgIcon = itemView.findViewById(R.id.img_icon);
        TextView tvTitle = itemView.findViewById(R.id.tv_title);

        tvTitle.setText(title);
        imgIcon.setImageResource(iconRes);
        // imgIcon.setBackgroundResource(R.drawable.bg_icon_blue); // Nếu em muốn nền icon màu xanh

        itemView.setOnClickListener(listener);
    }
}