package com.auction.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Quản lý chuyển đổi Scene và cửa sổ cho ứng dụng.
 */
public class SceneManager {
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Chuyển sang màn hình mới.
     * @param fxmlPath Đường dẫn tương đối từ thư mục resources, ví dụ: "/com/auction/client/views/LoginView.fxml"
     * @param title Tiêu đề cửa sổ
     * @return Controller của scene vừa load để truyền dữ liệu nếu cần
     */
    public static <T> T switchScene(String fxmlPath, String title) {
        try {
            URL url = SceneManager.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalStateException("Không tìm thấy file FXML: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            if (primaryStage.getScene() == null) {
                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
            } else {
                primaryStage.getScene().setRoot(root);
            }

            primaryStage.setTitle(title);
            primaryStage.centerOnScreen();
            primaryStage.show();

            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("Lỗi giao diện", "Không thể tải màn hình: " + fxmlPath);
            return null;
        }
    }
}
