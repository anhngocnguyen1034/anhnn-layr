Chào Claude, đây là mô tả chi tiết cấu trúc giao diện Màn hình HOME của ứng dụng chỉnh ảnh 'LAYR' được thiết kế theo Luồng tuyển tính (Linear Flow). Tôi muốn bạn viết code Jetpack Compose hoàn chỉnh cho màn hình này dựa trên các thành phần sau:

1. Tổng quan giao diện (Theme & Layout)
   Theme: Dark mode hoàn toàn. Màu nền (Background) là xám charcoal siêu tối (#121212 hoặc #1A1A1A). Các thẻ card dùng màu Surface xám nhẹ hơn (#222222) để tạo chiều sâu. Màu điểm nhấn (Primary) là xanh Teal hoặc Light Blue để làm nổi bật các nút bấm.

Layout chính: Dùng Column bọc trong một Modifier.verticalScroll để toàn bộ màn hình có thể cuộn mượt mà trên các dòng máy Android.

2. Chi tiết các thành phần từ trên xuống dưới (Top-to-Bottom Components)
   Thành phần 1: Top App Bar (Thanh tiêu đề)

Bố cục: Dùng Row nằm ngang, căn đều sang hai bên (Arrangement.SpaceBetween).

Bên trái: Text tên ứng dụng 'LAYR' viết hoa, font chữ đậm (Bold), bên cạnh có một icon lấp lánh (Sparkles/AutoAwesome).

Bên phải: Một icon cài đặt (Settings) hoặc icon tài khoản người dùng (AccountCircle) dạng hình tròn nhỏ.

Thành phần 2: Hero Section (Khu vực Nhập ảnh chính - Chiếm 40% màn hình phía trên)

Nền (Background): Sử dụng một hình ảnh chân dung lớn làm nền, được bo góc mịn (shape = RoundedCornerShape(24.dp)). Hình ảnh này được chia đôi theo chiều dọc bằng một đường line trắng mờ, giả lập hiệu ứng Trước/Sau (Before/After) của việc chỉnh ảnh để kích thích thị giác.

Nút Chụp ảnh (Primary CTA): Nằm chính giữa tấm ảnh nền. Thiết kế theo phong cách Glassmorphism (Kính mờ) bằng cách dùng Color.White.copy(alpha = 0.15f) kết hợp Modifier.blur(). Nút có dạng bo góc lớn, bên trong chứa một icon Máy ảnh (Icons.Rounded.CameraAlt) màu trắng và dòng chữ viết hoa 'CHỤP ẢNH NGAY'.

Nút Chọn từ thư viện: Nằm ngay dưới nút Chụp ảnh. Đây là một TextButton đơn giản màu trắng mờ với dòng chữ 'Chọn từ thư viện'. Khi người dùng click vào nút này hoặc nút Chụp ảnh, sẽ kích hoạt callback chuyển sang màn Editor.

Thành phần 3: Section 'ẢNH GẦN ĐÂY' (Recent Photos)

Tiêu đề: Gồm một Row chứa chữ 'ẢNH GẦN ĐÂY' (viết hoa, màu xám nhạt, font chữ nhỏ gọn) ở bên trái và chữ 'Xem tất cả' ở bên phải.

Danh sách ảnh: Dùng một LazyRow để cuộn ngang. Hiển thị 4-5 tấm ảnh chân dung mẫu dạng hình vuông hoặc chữ nhật đứng, bo góc (RoundedCornerShape(12.dp)), khoảng cách giữa các ảnh là 8.dp. Khi click vào bất kỳ ảnh nào cũng sẽ chuyển sang màn Editor với ảnh đó.

Thành phần 4: Section 'MẸO NHANH' (Quick Tutorials)

Tiêu đề: Chữ 'MẸO NHANH' viết hoa, kiểu dáng giống tiêu đề ảnh gần đây.

Danh sách mẹo: Hiển thị danh sách dạng cột đứng (Column). Mỗi mẹo là một Card nằm ngang, bo góc, gồm một icon nhỏ bên trái (ví dụ: hình cây kéo cho mẹo xóa nền, hình mặt cười cho mẹo làm nét) và phần Text bên phải gồm Tiêu đề mẹo (Bold) và mô tả ngắn 1 dòng bên dưới.

Thành phần 5: Bottom Navigation Bar (Thanh điều hướng dưới cùng)

Cố định ở đáy màn hình, gồm 3 vị trí: Trang chủ (Home - đang được active), Studio (Dự án của tôi), và Tài khoản (Profile).

Hãy viết code thật Clean, tối ưu hóa Recomposition bằng cách sử dụng remember, sử dụng thư viện Coil (AsyncImage) để load ảnh placeholder, và viết comment bằng tiếng Việt rõ ràng từng khu vực để tôi dễ dàng copy vào Android Studio.