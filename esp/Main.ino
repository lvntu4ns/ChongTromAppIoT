#include <ESP8266WiFi.h>        
#include <FirebaseESP8266.h> 
#include <time.h>              // Thư viện xử lý thời gian
#include <ArduinoJson.h>       // Thêm thư viện ArduinoJson

// Thông tin Wi-Fi
#define WIFI_SSID "Snut"
#define WIFI_PASSWORD "88888888"

// Firebase config
FirebaseData firebaseData;       // Đối tượng Firebase
FirebaseConfig config;           // Cấu hình Firebase
FirebaseAuth auth;               // Xác thực Firebase

// Chân kết nối
int pirPin = D1;
int buzzer = D2;
int dem = 1;
bool isAutoMode = false;

// Chân kết nối
#define RELAY_PIN D2  // Chân điều khiển thiết bị (Relay hoặc Buzzer)
// Trạng thái trước đó của switch
String lastSwitchState = "";

// Hàm lấy thời gian thực từ NTP
String getTime() {
  time_t now = time(nullptr);
  struct tm* timeinfo = localtime(&now);
  char buffer[30];
  strftime(buffer, sizeof(buffer), "%Y-%m-%d %H:%M:%S", timeinfo);
  return String(buffer);
}

void setup() {
  // Cấu hình chân
  pinMode(pirPin, INPUT);
  pinMode(buzzer, OUTPUT);
  digitalWrite(buzzer, LOW); // Đảm bảo còi tắt lúc khởi tạo
  Serial.begin(9600);        // Bắt đầu giao tiếp nối tiếp

  // Kết nối Wi-Fi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print("...");
    delay(500);
  }
  Serial.println("\nWi-Fi connected!");

  // Cấu hình Firebase
  config.host = "btliot-8e1a1-default-rtdb.asia-southeast1.firebasedatabase.app";
  config.signer.tokens.legacy_token = "IR7gTOVk8EFRYZbksOpAVojYZbS3NfVszXj6DymJ"; // Secret của Firebase

  // Kết nối Firebase
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  Serial.println("Firebase connected!");

  // Cấu hình NTP
  configTime(7 * 3600, 0, "pool.ntp.org", "time.nist.gov"); // UTC+7 cho Việt Nam
  Serial.println("NTP configured. Waiting for time...");
  while (time(nullptr) < 8 * 3600) {  // Đợi đồng bộ thời gian
    Serial.print(".");
    delay(500);
  }
  Serial.println("\nThời gian đã được đồng bộ!");
}

void loop() {
  // Đọc giá trị từ Firebase
  String state = "";
  String switchState = "";

  // Đọc giá trị "state"
  if (Firebase.getString(firebaseData, "/state")) {
    state = firebaseData.stringData();
    Serial.println("State: " + state);
  } else {
    Serial.println("Failed to read state: " + firebaseData.errorReason());
  }

  // Đọc trạng thái switch từ Firebase
  if (Firebase.getString(firebaseData, "/switch")) {
    String switchState = firebaseData.stringData();
    Serial.println("Switch state: " + switchState);

    // Xử lý trạng thái ON/OFF
    if (switchState != lastSwitchState) {  // Chỉ xử lý khi trạng thái thay đổi
      lastSwitchState = switchState;

      if (switchState.equalsIgnoreCase("ON")) {
        digitalWrite(RELAY_PIN, HIGH);  // Bật thiết bị
        Serial.println("Thiết bị đã bật!");
      } else if (switchState.equalsIgnoreCase("OFF")) {
        digitalWrite(RELAY_PIN, LOW);  // Tắt thiết bị
        Serial.println("Thiết bị đã tắt!");
      } else {
        Serial.println("Trạng thái không hợp lệ: " + switchState);
      }
    }
  } else {
    Serial.println("Lỗi khi đọc switch: " + firebaseData.errorReason());
  }

  // Cập nhật chế độ dựa trên state
  if (state.equalsIgnoreCase("AUTO")) {
    isAutoMode = true;
    Serial.println("Chuyển sang chế độ AUTO");
  } else if (state.equalsIgnoreCase("MANUAL")) {
    isAutoMode = false;
    Serial.println("Chuyển sang chế độ MANUAL");
  }

  // Xử lý chế độ Auto
  if (isAutoMode) {
    int pirState = digitalRead(pirPin);
    if (pirState == HIGH) {
      String message = "Phát hiện xâm nhập lần " + String(dem);
      Serial.println(message);
      delay(100);
      digitalWrite(buzzer, HIGH);
      delay(200);
      digitalWrite(buzzer, LOW);
      delay(200);
      dem++;

      // Lấy thời gian thực
      String timestamp = getTime(); 
      String firebaseMessage = "Phát hiện xâm nhập lúc " + timestamp;

      // Cập nhật thông tin lên Firebase
      String messagePath = "/motion/message/" + String(dem);
      if (Firebase.setString(firebaseData, messagePath, firebaseMessage)) {
        Serial.println("Đã cập nhật Firebase: " + firebaseMessage);
      } else {
        Serial.println("Lỗi khi cập nhật Firebase: " + firebaseData.errorReason());
      }

      // Lấy dữ liệu hiện tại từ Firebase
      if (Firebase.get(firebaseData, "/motion/message")) {
        // Chuyển dữ liệu Firebase thành JSON
        String jsonData = firebaseData.stringData();
        Serial.println("Dữ liệu JSON: " + jsonData);

        // Kiểm tra và đếm số lượng thông báo
        DynamicJsonDocument doc(1024);
        deserializeJson(doc, jsonData);

        size_t messageCount = doc.as<JsonObject>().size();
        Serial.print("Số lượng thông báo: ");
        Serial.println(messageCount);

        // Xóa thông báo cũ nhất nếu số lượng vượt quá 10
        if (messageCount > 10) {
          String deleteKey = String(messageCount - 10); // Lấy khóa của thông báo cũ nhất
          Firebase.deleteNode(firebaseData, "/motion/message/" + deleteKey);
          Serial.println("Đã xóa thông báo cũ nhất.");
        }
      } else {
        Serial.println("Lỗi khi đọc dữ liệu: " + firebaseData.errorReason());
      }
    }
  }

  delay(500); // Chờ đồng bộ Firebase
}
