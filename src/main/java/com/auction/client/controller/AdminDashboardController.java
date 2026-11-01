package com.auction.client.controller;

import com.auction.client.model.AuctionItem;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminDashboardController {

    @FXML private Label lblActiveSessions;
    @FXML private Label lblTotalUsers;
    @FXML private TableView<AuctionItem> tblPendingAuctions;
    @FXML private TableColumn<AuctionItem, String> colId;
    @FXML private TableColumn<AuctionItem, String> colTitle;
    @FXML private TableColumn<AuctionItem, Double> colStartPrice;
    @FXML private TableColumn<AuctionItem, String> colStatus;

    private final ObservableList<AuctionItem> pendingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        lblActiveSessions.setText("12 phiên đang chạy");
        lblTotalUsers.setText("1,450 người dùng trực tuyến");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        pendingList.add(new AuctionItem("PEN-101", "Tranh thủy mặc cổ thế kỷ 19", "Cần kiểm định", 50000000, 2000000, null, "CHỜ DUYỆT", 0));
        pendingList.add(new AuctionItem("PEN-102", "Xe máy Vespa cổ 1968", "Đầy đủ giấy tờ gốc", 80000000, 1000000, null, "CHỜ DUYỆT", 0));
        tblPendingAuctions.setItems(pendingList);
    }

    @FXML
    private void handleApprove() {
        AuctionItem selected = tblPendingAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Chưa chọn", "Vui lòng chọn 1 phiên để duyệt!");
            return;
        }
        pendingList.remove(selected);
        AlertUtils.showInfo("Thành công", "Đã phê duyệt phiên: " + selected.getTitle());
    }

    @FXML
    private void handleReject() {
        AuctionItem selected = tblPendingAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Chưa chọn", "Vui lòng chọn 1 phiên để từ chối!");
            return;
        }
        pendingList.remove(selected);
        AlertUtils.showInfo("Thông báo", "Đã từ chối phiên: " + selected.getTitle());
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/com/auction/client/views/AuctionListView.fxml", "Danh Sách Phiên Đấu Giá");
    }
}
