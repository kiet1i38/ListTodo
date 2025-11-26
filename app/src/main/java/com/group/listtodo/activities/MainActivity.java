package com.group.listtodo.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.group.listtodo.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 1. Mặc định khi mở app sẽ load màn hình Todoits (Danh sách công việc)
        if (savedInstanceState == null) {
            loadFragment(new TodoitsFragment());
        }

        // 2. Xử lý sự kiện bấm vào menu dưới đáy
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_todoits) {
                selectedFragment = new TodoitsFragment();
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (itemId == R.id.nav_quadrant) {
                selectedFragment = new QuadrantFragment();
            } else if (itemId == R.id.nav_more) {
                selectedFragment = new MoreFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    // Hàm thay thế Fragment vào khung FrameLayout
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}