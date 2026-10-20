package com.auction.model.entity;

/** Quản trị viên: quản lý user và phiên, không tự đấu giá hay bán. */
public class Admin extends User {
    public Admin(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
