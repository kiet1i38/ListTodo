package com.group.listtodo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.group.listtodo.R;
import com.group.listtodo.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPass;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. KIỂM TRA ĐĂNG NHẬP TỰ ĐỘNG (AUTO LOGIN)
        // Nếu đã có UserID trong máy -> Chuyển thẳng vào màn hình chính
        SessionManager session = new SessionManager(this);
        if (session.getUserId() != null) {
            goToMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ View
        edtEmail = findViewById(R.id.edt_email);
        edtPass = findViewById(R.id.edt_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.tv_register);
        TextView tvForgotPass = findViewById(R.id.tv_forgot_pass); // Nút quên mật khẩu

        // 2. CHUYỂN TRANG ĐĂNG KÝ
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // 3. CHUYỂN TRANG QUÊN MẬT KHẨU
        tvForgotPass.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // 4. XỬ LÝ ĐĂNG NHẬP
        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ Email và Mật khẩu!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Đăng nhập bằng Firebase
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // (Tùy chọn) Kiểm tra xem email đã xác thực chưa
                        /*
                        if (user != null && !user.isEmailVerified()) {
                            Toast.makeText(this, "Vui lòng vào Email để xác thực tài khoản trước!", Toast.LENGTH_LONG).show();
                            mAuth.signOut(); // Đăng xuất ngay
                            return;
                        }
                        */

                        if (user != null) {
                            // QUAN TRỌNG: Lưu UUID vào Session để dùng cho Database sau này
                            new SessionManager(this).saveUser(user.getUid());

                            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        }
                    } else {
                        // Hiển thị lỗi chi tiết (Sai pass, không có mạng...)
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Lỗi đăng nhập";
                        Toast.makeText(this, "Lỗi: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        // Xóa cờ activity cũ để không back lại màn hình login được
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}