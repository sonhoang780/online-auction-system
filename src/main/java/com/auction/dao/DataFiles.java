package com.auction.dao;

import com.auction.service.AuctionManager;

import java.nio.file.Path;

/**
 * Các DAO lưu file dùng chung cho cả server, dữ liệu nằm trong thư mục data/ (chạy server ở đâu thì tạo ở đó).
 * <p>
 * Phải dùng chung MỘT object cho mỗi file: nếu AuctionService và AuthService mỗi bên tự new FileUserDao
 * thì mỗi bên giữ một bản RAM riêng, bên này ghi file sẽ đè mất những gì bên kia vừa ghi.
 * Tạo lười (lần đầu gọi mới đọc file), nên test không dùng tới thì không đụng vào thư mục data/.
 */
public final class DataFiles {
    private static final Path DIR = Path.of("data");
    private static AuctionDao auctions;
    private static UserDao users;

    private DataFiles() {
    }

    public static synchronized AuctionDao auctions() {
        if (auctions == null) {
            auctions = new FileAuctionDao(DIR.resolve("auctions.dat"), AuctionManager.getInstance());
        }
        return auctions;
    }

    public static synchronized UserDao users() {
        if (users == null) {
            users = new FileUserDao(DIR.resolve("users.dat"));
        }
        return users;
    }
}
