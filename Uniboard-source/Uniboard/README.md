# Uniboard- Bàn phím gõ tiếng Việt

Uniboard là bàn phím Android (IME) gõ tiếng Việt bằng **Telex** và **VNI**, có chế độ **English**, hàng số `1234567890`, trang ký tự `?123` và logo riêng. Bố cục thu gọn khi xoay ngang; lựa chọn Telex/VNI/English được giữ lại khi IME khởi động lại.

## Tính năng

- Telex và VNI; hỗ trợ dấu sau âm tiết, ví dụ `khong6` → `không`.
- English mode để gõ nguyên văn tiếng Anh, email và mã.
- Hàng số `1234567890`; trong VNI, nhấn giữ số để chèn chính chữ số đó.
- Trang ký tự đặc biệt, Shift một lần viết hoa và nhấn lần nữa để khóa viết hoa.
- Giữ phím xóa để xóa liên tục.
- Giao diện thích ứng sáng/tối và bố cục ngang.

## Yêu cầu build

- Android Studio tương thích với Android Gradle Plugin 8.6.1.
- Android SDK Platform 35 và Build Tools.
- JDK 17 (JDK đi kèm Android Studio phù hợp).
- Gradle Wrapper tải Gradle 8.7 và kiểm tra SHA-256 của bản phân phối.

Mở thư mục gốc `Uniboard` trong Android Studio, chờ Gradle Sync, rồi dùng các lệnh dưới đây trong PowerShell.

## Build và cài bản debug

Từ thư mục gốc dự án:

```powershell
.\gradlew.bat assembleDebug
```

APK được tạo tại:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Cài lên thiết bị đang kết nối ADB:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Sau khi cài, mở Uniboard → **Bật Uniboard** → bật bàn phím trong Android Settings → **Chọn Uniboard**. Trên Samsung, mục này nằm trong phần **General management → Keyboard list and default** (tên có thể hơi khác theo phiên bản One UI).

## Build bản release đã ký

Không đưa keystore hoặc mật khẩu vào repo. Tạo keystore ở một thư mục riêng ngoài dự án; giữ bản sao an toàn ngoại tuyến. Ví dụ PowerShell:

```powershell
New-Item -ItemType Directory -Force D:\AndroidKeys
keytool -genkeypair -v -keystore D:\AndroidKeys\uniboard-release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias uniboard
```

`keytool` sẽ hỏi mật khẩu và thông tin chứng chỉ. Giữ nguyên keystore và alias cho các lần cập nhật tiếp theo; mất keystore có thể khiến bạn không ký được bản cập nhật cùng chữ ký.

Cấp thông tin ký cho phiên PowerShell hiện tại (các mật khẩu được hỏi thay vì ghi thẳng vào lệnh):

```powershell
$env:UNIBOARD_RELEASE_STORE_FILE = 'D:\AndroidKeys\uniboard-release.jks'
$env:UNIBOARD_RELEASE_KEY_ALIAS = 'uniboard'
$storeSecret = Read-Host 'Keystore password' -AsSecureString
$keySecret = Read-Host 'Key password' -AsSecureString
$env:UNIBOARD_RELEASE_STORE_PASSWORD = [System.Net.NetworkCredential]::new('', $storeSecret).Password
$env:UNIBOARD_RELEASE_KEY_PASSWORD = [System.Net.NetworkCredential]::new('', $keySecret).Password
.\gradlew.bat assembleRelease
```

Bản đã ký được tạo tại `app\build\outputs\apk\release\app-release.apk`. Nếu chưa đặt đủ bốn biến `UNIBOARD_RELEASE_*`, Gradle chỉ tạo `app-release-unsigned.apk`; APK unsigned không cài đặt/phát hành như bản release thông thường. Xóa mật khẩu khỏi môi trường sau build:

```powershell
'UNIBOARD_RELEASE_STORE_FILE','UNIBOARD_RELEASE_STORE_PASSWORD','UNIBOARD_RELEASE_KEY_ALIAS','UNIBOARD_RELEASE_KEY_PASSWORD' | ForEach-Object { Remove-Item "Env:\$_" -ErrorAction SilentlyContinue }
Remove-Variable storeSecret,keySecret -ErrorAction SilentlyContinue
```

Lấy SHA-256 fingerprint từ APK đã ký bằng `apksigner verify --print-certs`, rồi công bố fingerprint đó cùng bản phát hành để người dùng đối chiếu. Không phát hành APK debug.

## Quyền riêng tư

IME cần nhận phím để nhập văn bản. Uniboard không có lịch sử clipboard, từ điển cá nhân, đồng bộ đám mây hay ghi log nội dung phím; ứng dụng chỉ lưu lựa chọn chế độ gõ. Manifest tắt backup. Với các ô văn bản được Android cho phép xử lý Telex/VNI, IME đọc tối đa 12 ký tự trước con trỏ để ghép dấu. Password, ô số, URL, email, ô có cờ `IME_FLAG_NO_PERSONALIZED_LEARNING` và trường không xác định nhận phím nguyên dạng.

Ứng dụng đích có thể khai báo sai kiểu ô nhập; IME không thể tự nhận biết password được che nhưng khai báo như văn bản thường. Luôn bật bàn phím hệ thống trong app ngân hàng/trình quản lý mật khẩu nếu bạn không muốn dùng IME bên thứ ba ở đó. `FLAG_SECURE` của Uniboard chỉ bảo vệ cửa sổ bàn phím, không che nội dung cửa sổ của ứng dụng đang nhập.

## Đưa mã nguồn lên GitHub

Giải nén ZIP và mở PowerShell tại thư mục gốc `Uniboard`. Tạo một repository GitHub trống (không khởi tạo sẵn README), sau đó chạy:

```powershell
git init
git add .
git status --short
git commit -m "Initial Uniboard Android app"
git branch -M main
git remote add origin https://github.com/your-user/your-repo.git
git push -u origin main
```

Thay `your-user` và `your-repo` bằng tên tài khoản và repository GitHub của bạn. Kiểm tra `git status --short` trước khi commit. `.gitignore` loại build outputs, APK, `local.properties` và keystore khỏi Git.

Workflow `Gradle wrapper validation` tự kiểm tra Wrapper khi có push hoặc pull request. Chưa có giấy phép mã nguồn trong repo; hãy chọn và thêm `LICENSE` trước khi cho phép người khác sử dụng/phân phối dự án.

## Cấu hình Android

- `applicationId` hiện giữ là `com.vi.keyboa` để tiếp tục cập nhật ứng dụng đã cài.
- `minSdk 26`, `targetSdk 35`.
- `versionCode`/`versionName` được đặt trong `app/build.gradle.kts`.

