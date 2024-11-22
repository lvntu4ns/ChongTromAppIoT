package com.example.chongtrom;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "IntrusionAlertChannel";

    private Switch switchLight;
    private Button buttonAuto, buttonManual, buttonCancel;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Thiết lập Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Khởi tạo các nút và Firebase
        switchLight = findViewById(R.id.switchLight);
        buttonAuto = findViewById(R.id.buttonAuto);
        buttonManual = findViewById(R.id.buttonManual);
        buttonCancel = findViewById(R.id.buttonCancel);

        // Khởi tạo Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Thiết lập sự kiện cho chế độ Auto
        buttonAuto.setOnClickListener(v -> {
            databaseReference.child("state").setValue("auto");
            databaseReference.child("switch").setValue("none");
            switchLight.setVisibility(Switch.INVISIBLE);
            buttonAuto.setEnabled(false);
            buttonManual.setEnabled(true);
        });

        // Thiết lập sự kiện cho chế độ Manual
        buttonManual.setOnClickListener(v -> {
            databaseReference.child("state").setValue("manual");
            databaseReference.child("switch").setValue(switchLight.isChecked() ? "on" : "off");
            switchLight.setVisibility(Switch.VISIBLE);
            buttonManual.setEnabled(false);
            buttonAuto.setEnabled(true);
        });

        // Sự kiện cho nút Cancel (quay lại HomeActivity hoặc reset trạng thái)
        buttonCancel.setOnClickListener(v -> {
            databaseReference.child("state").setValue("none");  // Reset trạng thái hệ thống
            databaseReference.child("switch").setValue("off");  // Tắt công tắc

            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        // Lắng nghe trạng thái của Switch và cập nhật văn bản
        switchLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonManual.isEnabled()) {
                databaseReference.child("switch").setValue(isChecked ? "on" : "off");
            }
            switchLight.setText(isChecked ? "ON" : "OFF");
        });

        // Lắng nghe Firebase Realtime Database cho các thông báo xâm nhập
        databaseReference.child("motion/message").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Duyệt qua tất cả các thông báo trong "motion/message"
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    String intrusionMessage = messageSnapshot.getValue(String.class);
                    if (intrusionMessage != null && !intrusionMessage.isEmpty()) {
                        // Hiển thị thông báo trên thiết bị người dùng
                        displayNotification(intrusionMessage);

                        // Ghi thông báo vào file log
                        writeLogToFile(intrusionMessage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Xử lý khi có lỗi
            }
        });
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Intrusion Alert";
            String description = "Channel for intrusion alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void displayNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Phát hiện xâm nhập")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Tạo menu từ file menu_main.xml
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_history) {
            // Chuyển sang giao diện lịch sử
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Phương thức ghi log vào file trong thư mục của ứng dụng
    private void writeLogToFile(String message) {
        File logFile = new File(getExternalFilesDir(null), "warning_log.txt");

        try {
            // Tạo một FileOutputStream để ghi vào file
            FileOutputStream fos = new FileOutputStream(logFile, true); // true để append dữ liệu vào cuối file
            fos.write(message.getBytes());
            fos.write("\n".getBytes()); // Thêm dấu xuống dòng sau mỗi thông báo
            fos.close(); // Đóng stream sau khi ghi
        } catch (IOException e) {
            // Nếu có lỗi xảy ra
            e.printStackTrace();
        }
    }
}
