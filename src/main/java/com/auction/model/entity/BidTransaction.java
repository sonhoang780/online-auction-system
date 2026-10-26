package com.auction.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Một lượt đặt giá. Bất biến: tạo xong không sửa được, nên chia sẻ giữa các thread an toàn.
 * Là dữ liệu con (nằm bên trong từng phiên Auction để lưu lại lịch sử ai đã trả bao nhiêu tiền).
 */
public record BidTransaction(UUID auctionId, UUID bidderId, BigDecimal amount, LocalDateTime timestamp) {
    public BidTransaction {
        Objects.requireNonNull(auctionId, "auctionId must not be null");
        Objects.requireNonNull(bidderId, "bidderId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
