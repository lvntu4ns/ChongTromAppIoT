package com.example.chongtrom;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private Button buttonSystem, buttonLogout;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        buttonSystem = findViewById(R.id.buttonSystem);
        buttonLogout = findViewById(R.id.buttonLogout);

        // Chuyển đến MainActivity khi bấm "Hệ thống chống trộm"
        buttonSystem.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();  // Đóng HomeActivity để ngăn quay lại
        });

        // Đăng xuất
        buttonLogout.setOnClickListener(v -> {
            // Xóa trạng thái đăng nhập
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear(); // Xóa tất cả dữ liệu
            editor.apply();

            // Chuyển về LoginActivity
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
