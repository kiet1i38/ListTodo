package com.group.listtodo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.CountdownEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CountdownActivity extends AppCompatActivity {

    private LinearLayout containerLayout;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        db = AppDatabase.getInstance(this);

        // Ánh xạ Container (Nơi chứa danh sách)
        containerLayout = findViewById(R.id.container_events);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Nút FAB thêm mới
        findViewById(R.id.fab_add_event).setOnClickListener(v -> {
            startActivity(new Intent(this, AddCountdownActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents(); // Load lại dữ liệu mỗi khi quay lại màn hình
    }

    private void loadEvents() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<CountdownEvent> events = db.countdownDao().getAllEvents();

            runOnUiThread(() -> {
                // Giữ lại các View tĩnh (Banner...) nếu có, chỉ xóa các item động
                // Tuy nhiên để đơn giản, ta xóa hết các view con cũ rồi add lại từ đầu
                // Nếu em muốn giữ Banner tĩnh trong XML, hãy đặt banner ra ngoài LinearLayout này hoặc dùng RecyclerView

                // Ở đây thầy xóa các item cũ đi để tránh bị trùng lặp khi load lại
                if (containerLayout.getChildCount() > 0) {
                    containerLayout.removeAllViews();
                }

                // Add từng sự kiện từ DB vào
                for (CountdownEvent event : events) {
                    addEventView(event);
                }
            });
        });
    }

    private void addEventView(CountdownEvent event) {
        // 1. Inflate Layout (Lấy giao diện item_countdown.xml)
        View itemView = getLayoutInflater().inflate(R.layout.item_countdown, containerLayout, false);

        // 2. Ánh xạ các view trong item đó
        TextView tvIconChar = itemView.findViewById(R.id.tv_icon_char);
        TextView tvTitle = itemView.findViewById(R.id.tv_event_title);
        TextView tvDate = itemView.findViewById(R.id.tv_target_date);
        TextView tvDays = itemView.findViewById(R.id.tv_days_remaining);
        CardView cardIconBg = itemView.findViewById(R.id.card_icon_bg);

        // 3. Đổ dữ liệu
        tvTitle.setText(event.title);

        // Lấy chữ cái đầu làm Icon
        if (event.title != null && !event.title.isEmpty()) {
            tvIconChar.setText(String.valueOf(event.title.charAt(0)).toUpperCase());
        }

        // Format ngày tháng
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDate.setText("Mục tiêu: " + sdf.format(new Date(event.targetDate)));

        // Tính số ngày còn lại
        long diff = event.targetDate - System.currentTimeMillis();
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        // Xử lý hiển thị số ngày (Quá hạn hoặc Tương lai)
        if (days >= 0) {
            tvDays.setText(String.valueOf(days));
            tvDays.setTextColor(getResources().getColor(R.color.blue_primary));
            cardIconBg.setCardBackgroundColor(getResources().getColor(R.color.blue_primary));
        } else {
            tvDays.setText(String.valueOf(Math.abs(days))); // Hiện số dương
            TextView tvLabel = itemView.findViewById(R.id.tv_label_days);
            tvLabel.setText(" Ngày qua");
            tvLabel.setTextColor(getResources().getColor(R.color.quadrant_2_orange));
            tvDays.setTextColor(getResources().getColor(R.color.quadrant_2_orange));
            cardIconBg.setCardBackgroundColor(getResources().getColor(R.color.quadrant_2_orange));
        }

        // 4. Sự kiện Click vào item -> Sửa
        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCountdownActivity.class);
            intent.putExtra("event", event); // Truyền object sang AddCountdownActivity
            startActivity(intent);
        });

        // 5. Thêm vào danh sách
        containerLayout.addView(itemView);
    }
}