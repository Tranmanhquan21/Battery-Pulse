# Battery Pulse

Ứng dụng Android theo dõi thông số pin và trạng thái sạc theo thời gian thực. Giao diện chính tự động cập nhật khi Android phát hiện thay đổi pin.

## Trạng thái triển khai

### Đã làm được

- Dashboard hiển thị phần trăm pin, trạng thái sạc, nhiệt độ, điện áp, dòng điện và công suất ước tính.
- Thanh điều hướng cố định ở đáy chuyển giữa bốn màn hình độc lập: Trang chủ, Phân tích, Lịch sử và Cài đặt.
- Màn hình Lịch sử riêng có danh sách phiên sạc, chu kỳ tương đương và chỉ số sức khỏe ước lượng.
- Màn hình Phân tích riêng có biểu đồ pin, nhiệt độ, điện áp, dòng điện và công suất; nhãn đơn vị, giá trị thấp nhất/cao nhất và số mẫu đổi theo ngôn ngữ đã chọn.
- Ba thao tác nhanh được căn giữa theo hàng; **Làm mới** ở vị trí giữa và vẫn có thể dùng để đọc lại ngay lập tức.
- Khi Activity đang mở, giao diện tự động làm mới khi nhận thay đổi pin từ Android, có giới hạn tối thiểu 1 giây giữa hai lần ghi mẫu.
- Chọn ngôn ngữ English/Tiếng Việt và giao diện Dark/Light; các lựa chọn được lưu trong DataStore.
- Có thể bật/tắt cảnh báo khi pin đạt 80% lúc sạc và cảnh báo nhiệt độ từ 43°C; hai tùy chọn được lưu bằng DataStore.
- Có Room để lưu mẫu pin và phiên sạc cục bộ; dữ liệu vẫn còn sau khi tiến trình ứng dụng kết thúc.
- Có `BroadcastReceiver` nhận thay đổi trạng thái pin và sự kiện cắm/rút sạc để cập nhật phiên; Activity đăng ký receiver động để giao diện tự cập nhật khi đang mở.
- Có màn hình Lịch sử hiển thị các phiên sạc, thời gian, phần trăm tăng và nhiệt độ cao nhất.
- Màn hình Lịch sử tính chu kỳ sạc tương đương từ tổng phần trăm đã nạp và chỉ số sức khỏe pin ước lượng.
- Thông báo theo dõi nền và cảnh báo pin sử dụng ngôn ngữ hiện tại của ứng dụng; tên kênh thông báo có thể vẫn do Android giữ theo lần tạo kênh đầu tiên.
- Foreground Service lấy mẫu pin khoảng mỗi 30 giây khi bật theo dõi; khi Activity mở, `ACTION_BATTERY_CHANGED` tự động cập nhật nhanh hơn.
- Tối đa 2.000 mẫu biểu đồ được giữ trong bộ nhớ tiến trình, trong đó màn hình hiển thị tối đa 120 mẫu mới nhất.
- Dữ liệu UI được truyền bằng `StateFlow`; debug build chạy bằng Gradle.

### Chưa làm được

- Có thể tùy chỉnh ngưỡng cảnh báo phần trăm pin khi sạc và nhiệt độ; giá trị được giới hạn trong khoảng hợp lý và lưu bằng DataStore.
- Ước lượng tình trạng pin hiện là chỉ số tham khảo dựa trên dữ liệu phần trăm đã nạp, chưa phải Battery Health chính thức từ nhà sản xuất.
- Chưa có kiểm thử tự động riêng cho các chức năng pin, Room, phiên sạc và cảnh báo.

> Việc gửi thông báo yêu cầu người dùng cấp quyền thông báo trên Android 13 trở lên. Android thông thường không cho ứng dụng tự ngắt sạc ở mức 80%.

## Định hướng giao diện

Giao diện được phát triển theo hình ảnh tham khảo đã cung cấp, nhưng nội dung được chuyển sang ngữ cảnh theo dõi pin:

- **Điều hướng:** thanh tab cố định ở đáy cho Trang chủ, Phân tích, Lịch sử và Cài đặt.
- **Thao tác nhanh:** làm mới, bật theo dõi nền và mục Cảnh báo; Làm mới nằm chính giữa.
- **Live insights:** các thẻ nhiệt độ, điện áp, dòng điện và công suất.
- **Phân tích pin:** màn hình riêng với năm biểu đồ từ mẫu thu thập khi ứng dụng đọc trạng thái; mẫu mới được lưu vào Room.
- **Lịch sử sạc:** danh sách phiên sạc đã lưu, mức tăng phần trăm và nhiệt độ cao nhất.
- **Cài đặt:** ngôn ngữ, giao diện và công tắc cảnh báo pin/nhiệt độ.

Thiết kế giữ Dashboard cho thông tin cần xem nhanh; các màn hình chi tiết được truy cập qua thanh điều hướng riêng.

## Công nghệ

- **Kotlin:** ngôn ngữ chính cho Android.
- **Jetpack Compose + Material 3:** xây dựng UI khai báo và các thẻ giao diện.
- **Material Icons Extended:** cung cấp biểu tượng cho các chỉ số và hành động.
- **`BatteryManager` và `ACTION_BATTERY_CHANGED`:** đọc dữ liệu pin do Android cung cấp.
- **Foreground Service:** lấy mẫu dữ liệu nền khi người dùng bật theo dõi.
- **Kotlin Coroutines và `StateFlow`:** xử lý dữ liệu bất đồng bộ và cập nhật UI.
- **DataStore Preferences và Room:** lưu tùy chọn giao diện, cảnh báo, mẫu pin và phiên sạc cục bộ.
- **Gradle Kotlin DSL và Version Catalog:** cấu hình build và quản lý thư viện.

## Cấu trúc mã nguồn hiện tại

```text
app/src/main/java/com/example/batterypulse/
├── BatteryPulseApplication.kt
├── data/
│   ├── BatteryAlertNotifier.kt
│   ├── BatteryDatabase.kt
│   ├── BatteryEntities.kt
│   ├── BatteryDao.kt
│   ├── BatteryStateReceiver.kt
│   ├── BatteryMonitorService.kt
│   └── PreferencesRepository.kt
├── domain/
│   └── BatteryStatus.kt
└── ui/
    ├── MainActivity.kt
    ├── DashboardViewModel.kt
    └── DashboardViewModelFactory.kt
```

- `data/`: đọc dữ liệu pin, lưu Room, theo dõi nền, nhận sự kiện pin và lưu tùy chọn ngôn ngữ/giao diện.
- `domain/`: mô hình trạng thái pin.
- `ui/`: Activity, ViewModel và toàn bộ dashboard Compose hiện tại.

## Yêu cầu

- Android Studio hỗ trợ Android Gradle Plugin đang cấu hình trong dự án.
- Android SDK 37 theo cấu hình `compileSdk`.
- JDK tương thích với Gradle Wrapper.
- Thiết bị Android API 31 trở lên theo `minSdk`.

## Build và chạy

Mở dự án trong Android Studio, đồng bộ Gradle, sau đó chạy cấu hình `app` trên thiết bị hoặc emulator API 31 trở lên.

Build debug APK bằng Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK đầu ra:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Giới hạn hiện tại

- Dòng điện phụ thuộc vào việc từng thiết bị có cung cấp `BATTERY_PROPERTY_CURRENT_NOW` hay không.
- Công suất là giá trị ước tính từ điện áp và dòng điện tại thiết bị, không phải phép đo trực tiếp tại củ sạc.
- Foreground Service có thể bị giới hạn bởi chế độ tiết kiệm pin và chính sách chạy nền của từng nhà sản xuất.
- Giao diện hiện hỗ trợ English/Tiếng Việt và Dark/Light; các lựa chọn được lưu cục bộ bằng DataStore.
- Mẫu biểu đồ được lưu trong Room; giao diện chỉ hiển thị tối đa 120 mẫu mới nhất trong bộ nhớ hiện tại.
- Lịch sử phiên sạc được tạo từ các lần đọc trạng thái khi receiver, Activity đang mở hoặc foreground service cập nhật dữ liệu.
- Cảnh báo được kiểm tra mỗi khi trạng thái pin được làm mới; ngưỡng do người dùng chọn trong Cài đặt.
- Giao diện đã có thanh điều hướng riêng cho Trang chủ, Phân tích và Cài đặt.

## Lộ trình tiếp theo

1. Cho phép người dùng tùy chỉnh ngưỡng cảnh báo.
2. Cải thiện ước lượng tình trạng pin bằng dữ liệu dung lượng thực tế nếu thiết bị cung cấp.
3. Viết kiểm thử và kiểm tra trên nhiều hãng Android.

## Quyền riêng tư

Mục tiêu thiết kế là xử lý dữ liệu pin cục bộ trên thiết bị. Phiên bản hiện tại không cần tài khoản hoặc máy chủ để đọc và hiển thị thông số pin.
