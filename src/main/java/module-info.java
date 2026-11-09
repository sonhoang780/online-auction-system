module com.auction {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens com.auction to javafx.fxml;
    opens com.auction.server.protocol to com.google.gson;
    opens com.auction.model.entity to com.google.gson;

    exports com.auction;
    exports com.auction.server.protocol;
}
