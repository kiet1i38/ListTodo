package com.group.listtodo.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.group.listtodo.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtEmail, edtPass;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Ánh xạ View (Khớp với ID trong activity_register.xml)
        edtEmail = findViewById(R.id.edt_email);
        edtPass = findViewById(R.id.edt_password);
        Button btnReg = findViewById(R.id.btn_register);
        TextView tvLoginLink = findViewById(R.id.tv_login_link);

        // 3. Xử lý sự kiện bấm dòng chữ "Đã có tài khoản? Đăng nhập"
        tvLoginLink.setOnClickListener(v -> {
            finish(); // Đóng màn hình đăng ký để quay lại Login
        });

        // 4. Xử lý sự kiện bấm nút Đăng Ký
        btnReg.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPass.getText().toString().trim();

            // Validate dữ liệu đầu vào
            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Email!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pass.length() < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi hàm đăng ký
            registerUser(email, pass);
        });
    }

    private void registerUser(String email, String pass) {
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng ký thành công
                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                        finish(); // Quay lại màn hình Login để người dùng đăng nhập
                    } else {
                        // Đăng ký thất bại (VD: Email trùng, mạng lỗi...)
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                        Toast.makeText(RegisterActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}