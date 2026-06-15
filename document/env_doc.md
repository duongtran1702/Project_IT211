# 📄 Tài Liệu Cấu Hình Biến Môi Trường (Environment Variables Documentation)

Tài liệu này hướng dẫn cách cấu hình, hoạt động và các đoạn mã nguồn liên quan đến việc quản lý biến môi trường qua tệp `.env` trong dự án.

---

## 1. 📌 Tệp `.env` Là Gì? Vì Sao Cần Sử Dụng?
Tệp `.env` (Environment File) được đặt tại thư mục gốc của dự án, dùng để lưu trữ các thông tin cấu hình nhạy cảm (như mật khẩu cơ sở dữ liệu, API Key, Token Secret) hoặc các thông số thay đổi theo môi trường triển khai (Development, Staging, Production).

**Lợi ích:**
*   **Bảo mật:** Tránh việc hardcode thông tin nhạy cảm vào mã nguồn và vô tình đẩy lên các kho lưu trữ công khai (GitHub, GitLab).
*   **Tiện lợi:** Dễ dàng thay đổi cấu hình mà không cần phải biên dịch lại mã nguồn của ứng dụng.
*   *Lưu ý:* Tệp `.env` đã được đưa vào [.gitignore](file:///d:/IT211/Me/.gitignore) để không bị commit lên Git. Khi triển khai ở môi trường mới, người phát triển chỉ cần tạo tệp `.env` tương ứng dựa trên mẫu có sẵn.

---

## 2. 📊 Danh Sách Các Biến Môi Trường Hiện Tại
Dưới đây là bảng đặc tả các biến đang được định nghĩa trong tệp [.env](file:///d:/IT211/Me/.env) của hệ thống:

| Tên Biến | Kiểu Dữ Liệu | Mục Đích Sử Dụng | Ví Dụ Giá Trị |
| :--- | :--- | :--- | :--- |
| `DB_PASSWORD` | `String` | Mật khẩu truy cập cơ sở dữ liệu MySQL | `<db_password_của_bạn>` |
| `JWT_SECRET_KEY` | `String (Base64)` | Chìa khóa bí mật dùng để ký và xác thực JWT Token | `<chuỗi_base64_bí_mật>` |
| `CLOUDINARY_CLOUD_NAME` | `String` | Tên tài khoản lưu trữ đám mây Cloudinary | `<cloudinary_cloud_name>` |
| `CLOUDINARY_API_KEY` | `String` | API Key của Cloudinary | `<cloudinary_api_key>` |
| `CLOUDINARY_API_SECRET` | `String` | API Secret Key dùng để ký request Cloudinary | `<cloudinary_api_secret>` |
| `MAIL_USERNAME` | `String` | Tài khoản Email dùng để gửi thư hệ thống (SMTP) | `<email_gửi_thư>` |
| `MAIL_PASSWORD` | `String` | Mật khẩu ứng dụng (App Password) của Gmail SMTP | `<mật_khẩu_ứng_dụng_mail>` |

---

## 3. 🔌 Liên Kết Biến Môi Trường Trong Cấu Hình Spring Boot
Các biến môi trường nạp vào System Properties sẽ được tham chiếu trực tiếp trong file cấu hình [application.properties](file:///d:/IT211/Me/src/main/resources/application.properties) theo cú pháp `${TEN_BIEN}`:

```properties
# Cấu hình kết nối MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/project_it211_me?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}

# Cấu hình bảo mật JWT Token
jwt.secret-key=${JWT_SECRET_KEY}
jwt.access-expiration=900000
jwt.refresh-expiration=604800000

# Cấu hình tải ảnh lên Cloudinary
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME}
cloudinary.api-key=${CLOUDINARY_API_KEY}
cloudinary.api-secret=${CLOUDINARY_API_SECRET}
```

---

## 4. 💻 Các Đoạn Code Xử Lý Nạp Biến Môi Trường

### 4.1. Bộ Nạp Biến Môi Trường Tự Động Trong Java (`Application.java`)
Để ứng dụng có thể đọc được file `.env` khi chạy thực tế (không phụ thuộc vào thư viện bên thứ ba cồng kềnh), một bộ phân tích tệp `.env` gọn nhẹ đã được viết và đặt vào khối khởi tạo tĩnh `static {}` của lớp khởi chạy [Application.java](file:///d:/IT211/Me/src/main/java/atmin/Application.java):

```java
package atmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class Application {
    // Khối static chạy đầu tiên khi class Application được nạp vào JVM
    static {
        loadDotenv();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Đọc tệp .env ở thư mục gốc, phân tích từng dòng key=value 
     * và đưa vào System Properties của Java để Spring Boot tự động nhận diện.
     */
    private static void loadDotenv() {
        try {
            if (Files.exists(Paths.get(".env"))) {
                List<String> lines = Files.readAllLines(Paths.get(".env"));
                for (String line : lines) {
                    line = line.trim();
                    // Bỏ qua dòng trống hoặc dòng chú thích bắt đầu bằng dấu #
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        // Loại bỏ dấu nháy đơn hoặc nháy kép bao quanh giá trị (nếu có)
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        // Đăng ký biến vào môi trường hệ thống của Java JVM
                        System.setProperty(key, value);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load .env file: " + e.getMessage());
        }
    }
}
```

---

### 4.2. Bộ Nạp Biến Môi Trường Cho Môi Trường Kiểm Thử (`build.gradle`)
Khi chạy Unit/Integration Test (như lệnh `.\gradlew.bat test`), Gradle sẽ tự động tạo một tiến trình JVM độc lập để thực thi các test case. Tiến trình này không gọi hàm `main` của `Application.java`, dẫn đến việc các thuộc tính hệ thống chưa được nạp. 

Để khắc phục, chúng ta tích hợp bộ nạp `.env` ngay vào tiến trình kiểm thử của Gradle trong [build.gradle](file:///d:/IT211/Me/build.gradle):

```groovy
tasks.named('test') {
    useJUnitPlatform()

    // Đọc tệp .env ở thư mục gốc và đẩy các biến làm systemProperty cho JVM của Test task
    def envFile = file('.env')
    if (envFile.exists()) {
        envFile.eachLine { line ->
            line = line.trim()
            if (!line.isEmpty() && !line.startsWith('#')) {
                def parts = line.split('=', 2)
                if (parts.length == 2) {
                    def key = parts[0].trim()
                    def value = parts[1].trim()
                    // Loại bỏ dấu nháy bao quanh
                    if ((value.startsWith('"') && value.endsWith('"')) || 
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1)
                    }
                    // Đăng ký biến vào JVM chạy Test
                    systemProperty key, value
                }
            }
        }
    }
}
```

---

## 5. 🛠️ Hướng Dẫn Sử Dụng Cho Lập Trình Viên Mới (Onboarding Guide)
Khi một thành viên mới clone mã nguồn dự án về máy local, họ cần thực hiện các bước sau để thiết lập môi trường:

1.  Tạo một file mới tên là `.env` đặt ngay tại **thư mục gốc** của dự án (ngang hàng với `build.gradle`).
2.  Sao chép nội dung cấu hình mẫu dưới đây và điền các giá trị chính xác tương ứng trên máy cá nhân:
    ```env
    # Cấu hình database kết nối local
    DB_PASSWORD=mật_khẩu_mysql_của_bạn

    # Key bí mật của JWT (Có thể tạo ngẫu nhiên chuỗi Base64 dài)
    JWT_SECRET_KEY=chuỗi_base64_bí_mật_dùng_để_ký_token

    # Tài khoản Cloudinary của bạn dùng lưu trữ ảnh sân
    CLOUDINARY_CLOUD_NAME=tên_cloud
    CLOUDINARY_API_KEY=api_key_của_bạn
    CLOUDINARY_API_SECRET=api_secret_của_bạn

    # Tài khoản gửi mail SMTP
    MAIL_USERNAME=email_gửi_thư_hệ_thống@gmail.com
    MAIL_PASSWORD=mật_khẩu_ứng_dụng_16_ký_tự
    ```
3.  Lưu tệp tin `.env` lại và chạy lệnh kiểm tra biên dịch/chạy thử:
    ```bash
    # Chạy thử toàn bộ các kiểm thử tự động
    .\gradlew.bat clean test
    
    # Khởi chạy ứng dụng
    .\gradlew.bat bootRun
    ```
