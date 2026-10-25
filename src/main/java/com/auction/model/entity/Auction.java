package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Phiên đấu giá cho một Item.
 * Mọi thao tác đọc/ghi state đi qua cùng một ReentrantLock: nhiều client đặt giá
 * cùng lúc sẽ được xếp hàng, không có hai bid cùng thắng một mức giá.
 */
public class Auction extends Entity {
    private final UUID itemId;
    private final UUID sellerId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final BigDecimal minIncrement;

    private AuctionStatus status = AuctionStatus.OPEN;
    private BigDecimal currentPrice;
    private UUID leadingBidderId;              // null khi chưa có bid
    private final List<BidTransaction> bids = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime, BigDecimal minIncrement) {
        super();
        Objects.requireNonNull(item, "item must not be null");
        this.itemId = item.getId();
        this.sellerId = item.getSellerId();
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime phải sau startTime");
        }
        if (minIncrement == null || minIncrement.signum() <= 0) {
            throw new IllegalArgumentException("Bước giá phải > 0");
        }
        this.minIncrement = minIncrement;
        this.currentPrice = item.getStartingPrice();
    }

    // ============== DOMAIN OPERATIONS ==============

    public void start() {
        lock.lock();
        try {
            changeStatus(AuctionStatus.RUNNING);
        } finally {
            lock.unlock();
        }
    }

    public BidTransaction placeBid(Bidder bidder, BigDecimal amount) {
        Objects.requireNonNull(bidder, "bidder must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING || LocalDateTime.now().isAfter(endTime)) {
                throw new AuctionClosedException("Phiên " + getId() + " không nhận giá (" + status + ")");
            }
            if (!bidder.canBid()) {
                throw new InvalidBidException(bidder + " không có quyền đặt giá");
            }
            BigDecimal minAccepted = currentPrice.add(minIncrement);
            if (amount.compareTo(minAccepted) < 0) {
                throw new InvalidBidException("Giá phải >= " + minAccepted + ", nhận " + amount);
            }

            BidTransaction bid = new BidTransaction(getId(), bidder.getId(), amount, LocalDateTime.now());
            bids.add(bid);
            currentPrice = amount;
            leadingBidderId = bidder.getId();
            markUpdated();
            return bid;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Có bid: FINISHED, người thắng là leadingBidder. Không có bid: CANCELED.
     */
    public void finish() {
        lock.lock();
        try {
            changeStatus(bids.isEmpty() ? AuctionStatus.CANCELED : AuctionStatus.FINISHED);
        } finally {
            lock.unlock();
        }
    }

    public void markPaid() {
        lock.lock();
        try {
            changeStatus(AuctionStatus.PAID);
        } finally {
            lock.unlock();
        }
    }

    public void cancel() {
        lock.lock();
        try {
            changeStatus(AuctionStatus.CANCELED);
        } finally {
            lock.unlock();
        }
    }

    // Chỉ gọi khi đang giữ lock.
    private void changeStatus(AuctionStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Không thể chuyển " + status + " -> " + next);
        }
        status = next;
        markUpdated();
    }

    // ============== GETTERS ==============

    public UUID getItemId() {
        return itemId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public BigDecimal getMinIncrement() {
        return minIncrement;
    }

    public AuctionStatus getStatus() {
        lock.lock();
        try {
            return status;
        } finally {
            lock.unlock();
        }
    }

    public BigDecimal getCurrentPrice() {
        lock.lock();
        try {
            return currentPrice;
        } finally {
            lock.unlock();
        }
    }

    public UUID getLeadingBidderId() {
        lock.lock();
        try {
            return leadingBidderId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Bản sao lịch sử bid, sửa bản sao không ảnh hưởng phiên.
     */
    public List<BidTransaction> getBids() {
        lock.lock();
        try {
            return List.copyOf(bids);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "Auction{id=" + getId() + ", itemId=" + itemId + ", status=" + getStatus()
                + ", currentPrice=" + getCurrentPrice() + '}';
    }
}
