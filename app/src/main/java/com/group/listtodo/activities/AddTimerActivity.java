package com.group.listtodo.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.group.listtodo.R;
import com.group.listtodo.database.AppDatabase;
import com.group.listtodo.models.TimerPreset;
import com.group.listtodo.utils.SessionManager;
import com.group.listtodo.utils.SyncHelper;

import java.util.concurrent.Executors;

public class AddTimerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_timer);

        EditText edtH = findViewById(R.id.edt_h);
        EditText edtM = findViewById(R.id.edt_m);
        EditText edtS = findViewById(R.id.edt_s);
        EditText edtLabel = findViewById(R.id.edt_label);
        Button btnConfirm = findViewById(R.id.btn_confirm);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v -> {
            String h = edtH.getText().toString().isEmpty() ? "0" : edtH.getText().toString();
            String m = edtM.getText().toString().isEmpty() ? "0" : edtM.getText().toString();
            String s = edtS.getText().toString().isEmpty() ? "0" : edtS.getText().toString();
            String label = edtLabel.getText().toString();

            if (label.isEmpty()) {
                Toast.makeText(this, "Nhập tiêu đề đi!", Toast.LENGTH_SHORT).show();
                return;
            }

            long totalMs = (Long.parseLong(h) * 3600 + Long.parseLong(m) * 60 + Long.parseLong(s)) * 1000;

            TimerPreset timer = new TimerPreset(label, totalMs, R.drawable.ic_check_circle, R.color.blue_primary);
            timer.userId = new SessionManager(this).getUserId();

            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getInstance(this).timerDao().insert(timer);
                com.group.listtodo.utils.SyncHelper.autoBackup(this);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã thêm!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });
    }
}