package com.auction.model.entity;

/** Người bán: đăng sản phẩm và mở phiên đấu giá. */
public class Seller extends User {
    public Seller(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
    }

    @Override
    public Role getRole() {
        return Role.SELLER;
    }

    @Override
    public boolean canSell() {
        return true;
    }
}
