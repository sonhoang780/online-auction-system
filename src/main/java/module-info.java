module com.auction {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.auction to javafx.fxml;
    opens com.auction.client.controller to javafx.fxml;
    opens com.auction.client.model to javafx.base;

    exports com.auction;
    exports com.auction.client;
    exports com.auction.client.network;
}
