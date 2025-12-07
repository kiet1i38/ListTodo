package com.group.listtodo.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.group.listtodo.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtEmail, edtPass;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        edtEmail = findViewById(R.id.edt_email);
        edtPass = findViewById(R.id.edt_password);
        Button btnReg = findViewById(R.id.btn_register);
        TextView tvLoginLink = findViewById(R.id.tv_login_link);

        tvLoginLink.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        btnReg.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPass.getText().toString().trim();

            if (email.isEmpty() || pass.length() < 6) {
                Toast.makeText(this, "Email không hợp lệ hoặc mật khẩu quá ngắn (<6 ký tự)", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(email, pass);
        });
    }

    private void registerUser(String email, String pass) {
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this,
                                                    "Đăng ký thành công! Hãy kiểm tra Email để xác thực.",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                        finish();
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                    } else {
                        Toast.makeText(this, "Lỗi đăng ký: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
