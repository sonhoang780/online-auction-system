package com.auction.client.controller;

import com.auction.client.model.User;
import com.auction.client.network.ClientSocketService;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    public static User currentUser;

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            AlertUtils.showWarning("Cảnh báo", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        // Kiểm tra xem đang kết nối thật hay chạy demo
        ClientSocketService socketService = ClientSocketService.getInstance();
        if (!socketService.isSimulationMode()) {
            // Gửi bản tin JSON đăng nhập lên Server B
            String loginJson = String.format("{\"action\":\"LOGIN\",\"username\":\"%s\",\"password\":\"%s\"}", username, password);
            socketService.sendMessage(loginJson);
        }

        // Lưu thông tin phiên đăng nhập
        currentUser = new User(username, "BIDDER", 10000000);

        // Chuyển sang màn hình danh sách các phiên đấu giá
        SceneManager.switchScene("/com/auction/client/views/AuctionListView.fxml", "Danh Sách Phiên Đấu Giá - Hệ Thống Đấu Giá Realtime");
    }

    @FXML
    private void handleOpenRegister() {
        AlertUtils.showInfo("Đăng ký", "Chuyển sang màn hình Đăng ký tài khoản mới!");
    }

    @FXML
    private void handleEnableSimulation() {
        ClientSocketService.getInstance().enableSimulationMode(true);
        AlertUtils.showInfo("Chế độ thử nghiệm", "Đã bật chế độ Giả Lập (Mock Server). Bạn có thể test toàn bộ giao diện mà không cần Server B!");
    }
}
