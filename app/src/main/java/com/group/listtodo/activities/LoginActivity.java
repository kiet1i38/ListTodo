package com.group.listtodo.activities;

import android.app.ProgressDialog;
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
import com.group.listtodo.utils.SyncHelper; 

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPass;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingDialog; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);
        if (session.getUserId() != null) {
            goToMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Đang đăng nhập và đồng bộ dữ liệu...");
        loadingDialog.setCancelable(false);

        edtEmail = findViewById(R.id.edt_email);
        edtPass = findViewById(R.id.edt_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.tv_register);
        TextView tvForgotPass = findViewById(R.id.tv_forgot_pass);

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        tvForgotPass.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show(); 

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            new SessionManager(this).saveUser(uid);

                            SyncHelper.restoreData(this, uid, () -> {
                                runOnUiThread(() -> {
                                    loadingDialog.dismiss();
                                    Toast.makeText(this, "Đăng nhập & Đồng bộ thành công!", Toast.LENGTH_SHORT).show();
                                    goToMainActivity();
                                });
                            });
                        }
                    } else {
                        loadingDialog.dismiss();
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Lỗi đăng nhập";
                        Toast.makeText(this, "Lỗi: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
