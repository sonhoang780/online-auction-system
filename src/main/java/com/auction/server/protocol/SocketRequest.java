package com.auction.server.protocol;

import java.math.BigDecimal;

/**
 * Gói tin JSON từ Client gửi lên Server (Request).
 * Ví dụ:
 * - Đăng nhập: {"action":"LOGIN","username":"an","password":"123"}
 * - Đặt giá:   {"action":"PLACE_BID","auctionId":"...","amount":5500000}
 */
public class SocketRequest {
    private String action;

    // Dữ liệu xác thực (Auth)
    private String username;
    private String password;
    private String email;
    private String role; // BIDDER, SELLER, ADMIN

    // Dữ liệu đấu giá (Auction & Bid)
    private String auctionId;
    private BigDecimal amount;

    // Dữ liệu tạo phiên đấu giá mới (Seller)
    private String title;
    private String description;
    private String category;    // ELECTRONICS, ART, VEHICLE, OTHER
    private String condition;   // NEW, LIKE_NEW, USED
    private BigDecimal startPrice;
    private BigDecimal stepPrice;
    private Integer durationMinutes;

    public SocketRequest() {}

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public BigDecimal getStepPrice() {
        return stepPrice;
    }

    public void setStepPrice(BigDecimal stepPrice) {
        this.stepPrice = stepPrice;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
