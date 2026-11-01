package com.auction.client.controller;

import com.auction.client.model.AuctionItem;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class SellerDashboardController {

    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtStepPrice;
    @FXML private TextField txtDurationMinutes;

    @FXML private TableView<AuctionItem> tblMyItems;
    @FXML private TableColumn<AuctionItem, String> colId;
    @FXML private TableColumn<AuctionItem, String> colTitle;
    @FXML private TableColumn<AuctionItem, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionItem, String> colStatus;

    private final ObservableList<AuctionItem> myItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        myItems.add(new AuctionItem("SELLER-01", "Bàn phím cơ Custom Keychron Q1", "Switch Gateron Pro, Keycap PBT", 3200000, 100000, "chưa có", "ACTIVE", 1200));
        tblMyItems.setItems(myItems);
    }

    @FXML
    private void handleCreateAuction() {
        String title = txtTitle.getText().trim();
        String desc = txtDescription.getText().trim();
        String startPriceStr = txtStartPrice.getText().trim();
        String stepPriceStr = txtStepPrice.getText().trim();

        if (title.isEmpty() || startPriceStr.isEmpty() || stepPriceStr.isEmpty()) {
            AlertUtils.showWarning("Thiếu thông tin", "Vui lòng nhập đầy đủ tiêu đề, giá khởi điểm và bước giá!");
            return;
        }

        try {
            double startPrice = Double.parseDouble(startPriceStr);
            double stepPrice = Double.parseDouble(stepPriceStr);

            String newId = "SELLER-0" + (myItems.size() + 2);
            AuctionItem newItem = new AuctionItem(newId, title, desc, startPrice, stepPrice, "chưa có", "PENDING_APPROVAL", 3600);
            myItems.add(newItem);

            AlertUtils.showInfo("Thành công", "Đã gửi yêu cầu tạo phiên đấu giá mới! Chờ Admin phê duyệt.");

            txtTitle.clear();
            txtDescription.clear();
            txtStartPrice.clear();
            txtStepPrice.clear();
        } catch (NumberFormatException e) {
            AlertUtils.showError("Lỗi định dạng", "Giá tiền phải là số hợp lệ!");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/com/auction/client/views/AuctionListView.fxml", "Danh Sách Phiên Đấu Giá");
    }
}
