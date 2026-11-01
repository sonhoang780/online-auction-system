package com.auction.client.controller;

import com.auction.client.model.AuctionItem;
import com.auction.client.network.MockServerSimulator;
import com.auction.client.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Button;

public class AuctionListController {

    @FXML private Label lblWelcomeUser;
    @FXML private TableView<AuctionItem> tblAuctions;
    @FXML private TableColumn<AuctionItem, String> colId;
    @FXML private TableColumn<AuctionItem, String> colTitle;
    @FXML private TableColumn<AuctionItem, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionItem, Double> colStepPrice;
    @FXML private TableColumn<AuctionItem, String> colHighestBidder;
    @FXML private TableColumn<AuctionItem, String> colStatus;
    @FXML private TableColumn<AuctionItem, Void> colAction;

    private final ObservableList<AuctionItem> auctionData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (LoginController.currentUser != null) {
            lblWelcomeUser.setText("Xin chào, " + LoginController.currentUser.getUsername());
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStepPrice.setCellValueFactory(new PropertyValueFactory<>("stepPrice"));
        colHighestBidder.setCellValueFactory(new PropertyValueFactory<>("highestBidder"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Định dạng cột hiển thị tiền tệ
        colCurrentPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else setText(String.format("%,.0f VNĐ", price));
            }
        });

        // Nút bấm "Vào đấu giá" ở mỗi dòng
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnJoin = new Button("⚡ Đấu giá");
            {
                btnJoin.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");
                btnJoin.setOnAction(event -> {
                    AuctionItem item = getTableView().getItems().get(getIndex());
                    joinLiveAuction(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnJoin);
            }
        });

        loadSampleData();
        tblAuctions.setItems(auctionData);
    }

    private void loadSampleData() {
        auctionData.add(new AuctionItem("AUC-001", "iPhone 15 Pro Max 256GB LikeNew", "Máy nguyên bản, pin 100%", 22000000, 200000, "trung_nguyen", "ACTIVE", 300));
        auctionData.add(new AuctionItem("AUC-002", "Đồng hồ Rolex Submariner Date", "Hàng sưu tầm hộp sổ thẻ", 185000000, 1000000, "hoang_gia", "ACTIVE", 450));
        auctionData.add(new AuctionItem("AUC-003", "Tranh sơn dầu Hà Nội Phố cổ 1980", "Tác phẩm độc bản nghệ nhân", 15000000, 500000, "chua_co", "ACTIVE", 720));
        auctionData.add(new AuctionItem("AUC-004", "MacBook Pro M3 Max 64GB", "Màu Space Black, fullbox", 65000000, 500000, "tech_pro", "ENDED", 0));
    }

    private void joinLiveAuction(AuctionItem item) {
        LiveAuctionController controller = SceneManager.switchScene("/com/auction/client/views/LiveAuctionView.fxml", "Phòng Đấu Giá Trực Tiếp - " + item.getTitle());
        if (controller != null) {
            controller.setAuctionItem(item);
        }
        // Kích hoạt mô phỏng nhảy giá realtime cho phiên này
        MockServerSimulator.startRealtimeBidSimulation(item.getId());
    }

    @FXML
    private void handleOpenSellerDashboard() {
        SceneManager.switchScene("/com/auction/client/views/SellerDashboardView.fxml", "Quản Lý Sản Phẩm - Seller");
    }

    @FXML
    private void handleOpenAdminDashboard() {
        SceneManager.switchScene("/com/auction/client/views/AdminDashboardView.fxml", "Bảng Điều Khiển Admin");
    }

    @FXML
    private void handleLogout() {
        LoginController.currentUser = null;
        SceneManager.switchScene("/com/auction/client/views/LoginView.fxml", "Đăng nhập hệ thống Đấu giá");
    }
}
