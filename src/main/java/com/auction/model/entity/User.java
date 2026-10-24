package com.auction.model.entity;

import java.util.regex.Pattern;

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

    /**
     * QUY TẮC ĐẶT EMAIL
     * Cho phép local part: chữ, số, dấu chấm, gạch dưới, gạch ngang, plus, %.
     * Domain phải có ít nhất 1 dấu chấm và TLD ≥ 2 chữ.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    protected User(String username, String passwordHash, String email) {
        this.username = requireText(username, "username");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.email = validateEmail(email);
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

    private static String validateEmail(String email) {
        String trimmed = email.trim();
        // RFC 5321: tổng độ dài địa chỉ email tối đa 254 ký tự
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email không được để trống");
        }
        if (trimmed.length() > 254) {
            throw new IllegalArgumentException("Email vuot qua 254 ky tu");
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Email khong hop le");
        }
        return trimmed;
    }

}
