# Hướng dẫn cài đặt môi trường & clone repo BTL

Repo: **https://github.com/sonhoang780/online-auction-system**

Dự án dùng **Maven + JavaFX 21**. JavaFX được Maven tự tải về như một thư viện, nên **không cần tải/cài JavaFX SDK riêng**, không cần thêm VM options `--module-path`. Chỉ cần JDK, Git và IntelliJ.

---

## 1. Cài phần mềm cần thiết

| Phần mềm | Phiên bản | Link |
|---|---|---|
| JDK | **17 trở lên** (khuyên dùng 21) | https://adoptium.net (chọn Temurin 21 LTS) |
| Git | mới nhất | https://git-scm.com/downloads |
| IntelliJ IDEA | Community hoặc Ultimate | https://www.jetbrains.com/idea/download |
| SceneBuilder (tuỳ chọn) | mới nhất | https://gluonhq.com/products/scene-builder/ |

**Lưu ý khi cài JDK (Temurin) trên Windows:** ở bước chọn tính năng, bật **"Set JAVA_HOME variable"** và **"Add to PATH"**.

Kiểm tra sau khi cài (mở terminal mới):

```bash
java -version    # phải ra 17 hoặc cao hơn
git --version
```

> Không cần cài Maven: repo có sẵn Maven Wrapper (`mvnw` / `mvnw.cmd`), lần chạy đầu sẽ tự tải Maven và JavaFX.

## 2. Cấu hình Git (làm 1 lần)

```bash
git config --global user.name "Tên Của Bạn"
git config --global user.email "email-dang-ky-github@example.com"
```

Email nên trùng email tài khoản GitHub để commit hiện đúng tên.

## 3. Nhận quyền push vào repo

1. Gửi **username GitHub** cho trưởng nhóm để được mời làm collaborator.
2. Mở email (hoặc https://github.com/notifications) → bấm **Accept invitation**.

Chưa accept thì vẫn clone được nhưng **không push được**.

## 4. Clone repo

```bash
git clone https://github.com/sonhoang780/online-auction-system.git
cd online-auction-system
git switch dev
```

Mọi người làm việc trên nhánh **`dev`**, không code trực tiếp trên `main`.

Lần push đầu tiên, Windows sẽ hiện cửa sổ đăng nhập GitHub → chọn **Sign in with your browser**.

## 5. Mở và chạy project

### Cách 1: IntelliJ (khuyên dùng)

1. **File → Open** → chọn thư mục `online-auction-system` (hoặc file `pom.xml`) → **Open as Project**.
2. Đợi IntelliJ tải Maven dependencies (thanh tiến trình góc dưới phải).
3. **File → Project Structure → Project → SDK**: chọn JDK 17/21 vừa cài.
4. Mở tab **Maven** (thanh bên phải) → `online-auction-system` → **Plugins → javafx → javafx:run** (double-click).

Hiện cửa sổ "Online Auction System" là thành công.

### Cách 2: Terminal

```bash
mvnw.cmd javafx:run      # Windows (cmd)
.\mvnw.cmd javafx:run    # Windows (PowerShell)
./mvnw javafx:run        # macOS / Linux / Git Bash
```

## 6. Quy trình làm việc hằng ngày

### Vì sao mỗi người phải tạo nhánh riêng?

Mỗi người code trên **nhánh của mình**, tách ra từ `dev`, không sửa trực tiếp vào `dev`:

```
main ─────────────────────────────────── (bản ổn định, nộp bài)
  └─ dev ──────●──────────●────────────── (code chung của nhóm)
               │          ↑ merge qua Pull Request
               ├─ feature/login-view ─●─●  (bạn A làm màn đăng nhập)
               └─ feature/bid-panel ──●──  (bạn B làm phần đặt giá)
```

- A và B code song song, không ghi đè code của nhau.
- Code hỏng thì chỉ hỏng nhánh riêng, `dev` vẫn chạy được.
- Làm xong mới gộp vào `dev` qua Pull Request, có người review trước.

Tên nhánh: `feature/<việc-đang-làm>` khi làm tính năng mới, `fix/<tên-lỗi>` khi sửa lỗi. Viết thường, không dấu, nối bằng `-`.

### Các bước

```bash
# 1. Chuyển về dev và kéo code mới nhất của cả nhóm
git switch dev
git pull

# 2. Tạo nhánh mới TỪ dev (vì đang đứng ở dev) và chuyển sang nhánh đó luôn
#    -c = create (tạo mới). Tương đương lệnh cũ: git checkout -b ...
git switch -c feature/ten-tinh-nang     # vd: feature/login-view

# 3. Code xong thì commit
git add .
git commit -m "feat: thêm màn hình đăng nhập"

# 4. Đẩy nhánh lên GitHub
git push -u origin feature/ten-tinh-nang
```

5. Lên GitHub → bấm **Compare & pull request** → chọn base là **`dev`** (không phải `main`) → tạo PR, nhờ người khác review rồi merge.

Quy ước commit message: `feat:` (tính năng mới), `fix:` (sửa lỗi), `docs:` (tài liệu), `refactor:`, `chore:` (cấu hình, linh tinh).

## 7. Lỗi thường gặp

| Lỗi | Cách sửa |
|---|---|
| `JAVA_HOME not found` / `java is not recognized` | Cài lại JDK có tick "Set JAVA_HOME" và "Add to PATH", rồi **mở lại terminal/IntelliJ**. |
| `'mvnw' is not recognized` (PowerShell) | Dùng `.\mvnw.cmd javafx:run`. |
| `JavaFX runtime components are missing` | Đang chạy sai cách. Chạy bằng **Maven → javafx:run** như mục 5. |
| `invalid target release: 17` | Project SDK đang là JDK cũ (8, 11). Đổi sang JDK 17+ ở Project Structure. |
| `Permission denied` / `403` khi push | Chưa accept lời mời collaborator, hoặc đăng nhập nhầm tài khoản GitHub (xoá trong **Windows Credential Manager → github.com** rồi push lại). |
| IntelliJ báo đỏ hết các `import javafx...` | Bấm **Reload All Maven Projects** (icon xoay tròn trong tab Maven). |
| Conflict khi `git pull` | Không tự xử được thì nhắn nhóm, **đừng** dùng `git push --force`. |
