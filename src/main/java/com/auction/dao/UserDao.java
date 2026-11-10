package com.auction.dao;

import com.auction.model.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Nơi lưu người dùng (Bidder, Seller, Admin). Đăng ký/đăng nhập (AuthService của B) đi qua đây. */
public interface UserDao {
    /** Thêm mới hoặc ghi đè user cùng id. Gọi lại sau khi đổi số dư để bản lưu có số dư mới. */
    void save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    List<User> findAll();
}
