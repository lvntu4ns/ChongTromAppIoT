package com.example.chongtrom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin;
    private CheckBox checkboxRememberMe;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo các view
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        checkboxRememberMe = findViewById(R.id.checkboxRememberMe);

        // Khởi tạo Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Kiểm tra trạng thái đăng nhập lưu trữ
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean isRemembered = sharedPreferences.getBoolean("rememberMe", false);
        String savedUsername = sharedPreferences.getString("username", null);
        String savedPassword = sharedPreferences.getString("password", null);

        if (isRemembered && savedUsername != null && savedPassword != null) {
            // Nếu đã lưu thông tin, tự động đăng nhập
            loginUser(savedUsername, savedPassword, true);
        }

        // Đăng nhập khi nhấn nút Login
        buttonLogin.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            // Kiểm tra nếu username và password không rỗng
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hash mật khẩu với MD5
            String hashedPassword = md5(password);

            // Kiểm tra đăng nhập
            loginUser(username, hashedPassword, checkboxRememberMe.isChecked());
        });
    }

    // Hàm hash mật khẩu MD5
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Hàm kiểm tra đăng nhập
    private void loginUser(String username, String password, boolean rememberMe) {
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Kiểm tra nếu dữ liệu tồn tại
                if (dataSnapshot.exists()) {
                    String storedUsername = dataSnapshot.child("username").getValue(String.class);
                    String storedPassword = dataSnapshot.child("passwd").getValue(String.class);

                    // Kiểm tra nếu username và password khớp
                    if (storedUsername != null && storedPassword != null && storedUsername.equals(username) && storedPassword.equals(password)) {
                        // Lưu trạng thái nếu cần
                        if (rememberMe) {
                            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("rememberMe", true);
                            editor.putString("username", username);
                            editor.putString("password", password);
                            editor.apply();
                        }

                        // Đăng nhập thành công
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Nếu mật khẩu sai
                        Toast.makeText(LoginActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Nếu không tìm thấy user
                    Toast.makeText(LoginActivity.this, "Tên đăng nhập không tồn tại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Firebase", "Error: " + databaseError.getMessage());
            }
        });
    }
}
