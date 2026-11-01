package com.auction.client;

import javafx.application.Application;

/**
 * Lớp khởi chạy bọc ngoài (Launcher Wrapper).
 * Giúp giải quyết lỗi kinh điển: "JavaFX runtime components are missing" trên IntelliJ & Java 11+.
 */
public class MainLauncher {
    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}
