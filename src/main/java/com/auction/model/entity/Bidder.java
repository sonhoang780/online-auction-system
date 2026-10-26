package com.auction.model.entity;

import java.math.BigDecimal;

/**
 * Người đấu giá, có số dư để trả tiền khi thắng phiên.
 * Các hàm đụng tới balance là synchronized: nhiều thread (nhiều phiên) có thể
 * cùng trừ/cộng tiền của một người, không khóa sẽ bị mất cập nhật.
 */
public class Bidder extends User {
    private BigDecimal balance = BigDecimal.ZERO;

    public Bidder(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
    }

    @Override
    public Role getRole() {
        return Role.BIDDER;
    }

    @Override
    public boolean canBid() {
        return true;
    }

    public synchronized BigDecimal getBalance() {
        return balance;
    }

    public synchronized void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public synchronized void withdraw(BigDecimal amount) {
        requirePositive(amount);
        // BigDecimal không được dùng với equals vì:
        //Ví dụ: new BigDecimal("2.0").equals(new BigDecimal("2.00"))
        //→ trả về false (vì khác số chữ số 0 đằng sau).
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Số dư không đủ: có " + balance + ", cần " + amount);
        }
        balance = balance.subtract(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền phải > 0");
        }
    }
}
