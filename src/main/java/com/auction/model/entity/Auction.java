package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.observer.AuctionListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Phiên đấu giá cho một Item.
 * Mọi thao tác đọc/ghi state đi qua cùng một ReentrantLock: nhiều client đặt giá
 * cùng lúc sẽ được xếp hàng, không có hai bid cùng thắng một mức giá.
 * Listener được báo SAU khi nhả lock: listener chậm (gửi mạng) không giữ chân các bid khác,
 * và listener gọi ngược lại Auction từ thread khác cũng không bị deadlock.
 */
public class Auction extends Entity {
    private static final System.Logger LOG = System.getLogger(Auction.class.getName());

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
    // Đọc nhiều (mỗi bid duyệt 1 lần), ghi ít (client vào/ra phiên) nên không cần lock riêng.
    private final List<AuctionListener> listeners = new CopyOnWriteArrayList<>();

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

    // ============== OBSERVER ==============

    public void addListener(AuctionListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    public void removeListener(AuctionListener listener) {
        listeners.remove(listener);
    }

    // Hai hàm notify chỉ gọi khi KHÔNG giữ lock. Một listener lỗi không được làm hỏng
    // bid đã ghi hay chặn các listener còn lại, nên chỉ log rồi đi tiếp.
    private void notifyBidPlaced(BidTransaction bid) {
        for (AuctionListener listener : listeners) {
            try {
                listener.onBidPlaced(this, bid);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Listener lỗi ở phiên " + getId(), e);
            }
        }
    }

    private void notifyStatusChanged(AuctionStatus oldStatus, AuctionStatus newStatus) {
        for (AuctionListener listener : listeners) {
            try {
                listener.onStatusChanged(this, oldStatus, newStatus);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Listener lỗi ở phiên " + getId(), e);
            }
        }
    }

    // ============== DOMAIN OPERATIONS ==============

    public void start() {
        AuctionStatus old;
        lock.lock();
        try {
            old = changeStatus(AuctionStatus.RUNNING);
        } finally {
            lock.unlock();
        }
        notifyStatusChanged(old, AuctionStatus.RUNNING);
    }
    public boolean startIfDue() {
        AuctionStatus old;

        lock.lock();
        try {
            if (status != AuctionStatus.OPEN
                    || LocalDateTime.now().isBefore(startTime)) {
                return false;
            }

            old = changeStatus(AuctionStatus.RUNNING);
        } finally {
            lock.unlock();
        }

        notifyStatusChanged(old, AuctionStatus.RUNNING);
        return true;
    }

    public BidTransaction placeBid(Bidder bidder, BigDecimal amount) {
        BidTransaction bid = recordBid(bidder, amount);
        notifyBidPlaced(bid);
        return bid;
    }

    private BidTransaction recordBid(Bidder bidder, BigDecimal amount) {
        Objects.requireNonNull(bidder, "bidder must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();

            // Kiểm tra trạng thái và thời gian đấu giá
            if (status != AuctionStatus.RUNNING
                    || now.isBefore(startTime)
                    || !now.isBefore(endTime)) {
                throw new AuctionClosedException(
                        "Phiên " + getId() + " không nhận giá (" + status + ")"
                );
            }

            // Kiểm tra quyền đặt giá
            if (!bidder.canBid()) {
                throw new InvalidBidException(
                        bidder + " không có quyền đặt giá"
                );
            }

            // Kiểm tra mức giá tối thiểu
            BigDecimal minAccepted = currentPrice.add(minIncrement);

            if (amount.compareTo(minAccepted) < 0) {
                throw new InvalidBidException(
                        "Giá phải >= " + minAccepted + ", nhận " + amount
                );
            }

            // Ghi nhận lượt đặt giá
            BidTransaction bid = new BidTransaction(
                    getId(),
                    bidder.getId(),
                    amount,
                    now
            );

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
        AuctionStatus old;
        AuctionStatus next;
        lock.lock();
        try {
            // bids.isEmpty() phải đọc trong lock, nếu không một bid chen vào giữa sẽ bị CANCELED oan.
            next = bids.isEmpty() ? AuctionStatus.CANCELED : AuctionStatus.FINISHED;
            old = changeStatus(next);
        } finally {
            lock.unlock();
        }
        // Ngoài finally: chỉ báo khi đổi trạng thái thành công, try ném lỗi thì không chạy tới đây.
        notifyStatusChanged(old, next);
    }
    public boolean finishIfExpired() {
        AuctionStatus old;
        AuctionStatus next;

        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING
                    || LocalDateTime.now().isBefore(endTime)) {
                return false;
            }

            next = bids.isEmpty()
                    ? AuctionStatus.CANCELED
                    : AuctionStatus.FINISHED;

            old = changeStatus(next);
        } finally {
            lock.unlock();
        }

        notifyStatusChanged(old, next);
        return true;
    }

    public void markPaid() {
        AuctionStatus old;
        lock.lock();
        try {
            old = changeStatus(AuctionStatus.PAID);
        } finally {
            lock.unlock();
        }
        // Ngoài finally: chỉ báo khi đổi trạng thái thành công, try ném lỗi thì không chạy tới đây.
        notifyStatusChanged(old, AuctionStatus.PAID);
    }

    public void cancel() {
        AuctionStatus old;
        lock.lock();
        try {
            old = changeStatus(AuctionStatus.CANCELED);
        } finally {
            lock.unlock();
        }
        // Ngoài finally: chỉ báo khi đổi trạng thái thành công, try ném lỗi thì không chạy tới đây.
        notifyStatusChanged(old, AuctionStatus.CANCELED);
    }

    // Chỉ gọi khi đang giữ lock. Trả về trạng thái cũ để báo listener sau khi nhả lock.
    private AuctionStatus changeStatus(AuctionStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Không thể chuyển " + status + " -> " + next);
        }
        AuctionStatus old = status;
        status = next;
        markUpdated();
        return old;
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
