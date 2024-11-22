package com.example.chongtrom;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private ListView listViewHistory;
    private DatabaseReference databaseReference;
    private ArrayList<String> messageList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Thiết lập Toolbar nếu cần
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Đặt tiêu đề cho Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Lịch sử cảnh báo");  // Đặt tiêu đề đúng cho Toolbar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // Hiển thị nút quay lại
        }
        // Đặt màu chữ trắng cho tiêu đề
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        // Khởi tạo ListView và ArrayList
        listViewHistory = findViewById(R.id.listViewHistory);
        messageList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageList);
        listViewHistory.setAdapter(adapter);

        // Khởi tạo Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Lắng nghe thông tin xâm nhập từ Firebase
        databaseReference.child("motion").child("message").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messageList.clear();  // Xóa danh sách cũ trước khi cập nhật

                // Kiểm tra nếu Firebase có nhiều thông báo
                if (snapshot.exists()) {
                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                        String message = messageSnapshot.getValue(String.class);
                        if (message != null) {
                            messageList.add(0, message);  // Thêm thông báo vào danh sách
                        }
                    }
                }
                adapter.notifyDataSetChanged();  // Cập nhật ListView
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Xử lý lỗi nếu có
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Xử lý nút back trên Toolbar
        onBackPressed();
        return true;
    }
}
