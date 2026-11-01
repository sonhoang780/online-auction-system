package com.auction.client;

import com.auction.client.network.ClientSocketService;
import com.auction.client.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry Point chính của ứng dụng Client JavaFX (Module C).
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.setPrimaryStage(primaryStage);

        // Mặc định bật chế độ giả lập để người dùng hoặc nhóm trưởng có thể chạy thử ngay giao diện
        // Khi ghép nối với Server B (Tuần 11), tắt dòng này và gọi ClientSocketService.getInstance().connect("localhost", 8888)
        ClientSocketService.getInstance().enableSimulationMode(true);

        // Khởi động màn hình Đăng nhập đầu tiên
        SceneManager.switchScene("/com/auction/client/views/LoginView.fxml", "Hệ Thống Đấu Giá Trực Tuyến - Đăng Nhập");
    }

    @Override
    public void stop() {
        // Đóng socket và luồng ngầm an toàn khi người dùng bấm dấu [X] tắt cửa sổ
        ClientSocketService.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
