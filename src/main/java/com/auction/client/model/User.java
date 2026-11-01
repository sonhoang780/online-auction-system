package com.auction.client.model;

/**
 * Model biểu diễn thông tin người dùng đã đăng nhập vào hệ thống.
 */
public class User {
    private String username;
    private String role; // "BIDDER", "SELLER", "ADMIN"
    private double balance;

    public User(String username, String role, double balance) {
        this.username = username;
        this.role = role;
        this.balance = balance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", balance=" + balance +
                '}';
    }
}
