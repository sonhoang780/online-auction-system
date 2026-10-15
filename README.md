# Online Auction System

Bài tập lớn Lập trình nâng cao 2025–2026: hệ thống đấu giá trực tuyến (Client–Server, JavaFX, MVC).

> **Thành viên mới:** đọc [Hướng dẫn cài đặt & clone repo](docs/SETUP.md) trước.

## Yêu cầu

- JDK 17 trở lên
- Không cần cài Maven: dùng Maven Wrapper (`mvnw`) có sẵn trong repo
- (Tuỳ chọn) [SceneBuilder](https://gluonhq.com/products/scene-builder/) để sửa file `.fxml`

## Chạy

```bash
./mvnw javafx:run        # macOS/Linux/Git Bash
mvnw.cmd javafx:run      # Windows cmd/PowerShell
```

Trong IntelliJ: mở thư mục repo (Open → chọn `pom.xml`), chạy `HelloApplication`.

## Quy trình Git

- `main`: bản ổn định, chỉ merge từ `dev` qua Pull Request.
- `dev`: nhánh tích hợp. Mỗi tính năng tạo nhánh `feature/<tên>` từ `dev`, xong mở PR về `dev`.
- Commit theo [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, ...).
