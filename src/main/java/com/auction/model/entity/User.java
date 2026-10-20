package com.auction.model.entity;

/**
 * Người dùng hệ thống. Mỗi vai là một lớp con, quyền được quyết định bằng
 * đa hình (canBid/canSell) thay vì if-else theo vai ở nơi gọi.
 */
public abstract class User extends Entity {
    public enum Role { BIDDER, SELLER, ADMIN }

    private final String username;
    // Chỉ lưu mật khẩu đã băm; việc băm/kiểm tra thuộc phần đăng nhập phía server.
    private final String passwordHash;
    private final String email;

    protected User(String username, String passwordHash, String email) {
        this.username = requireText(username, "username");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.email = requireText(email, "email");
    }

    public abstract Role getRole();

    public boolean canBid() {
        return false;
    }

    public boolean canSell() {
        return false;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        // Không in passwordHash ra log.
        return getRole() + "(" + username + ")";
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " không được để trống");
        }
        return value;
    }
}
