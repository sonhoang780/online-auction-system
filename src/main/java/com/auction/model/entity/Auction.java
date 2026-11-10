package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.observer.AuctionListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.time.Duration;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

/**
 * Phiên đấu giá cho một Item.
 * Mọi thao tác đọc/ghi state đi qua cùng một ReentrantLock: nhiều client đặt giá
 * cùng lúc sẽ được xếp hàng, không có hai bid cùng thắng một mức giá.
 * Listener được báo SAU khi nhả lock: listener chậm (gửi mạng) không giữ chân các bid khác,
 * và listener gọi ngược lại Auction từ thread khác cũng không bị deadlock.
 */
public class Auction extends Entity {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final System.Logger LOG = System.getLogger(Auction.class.getName());
    private static final Duration ANTI_SNIPING_WINDOW = Duration.ofSeconds(30);
    private static final Duration ANTI_SNIPING_EXTENSION = Duration.ofSeconds(30);

    private final UUID itemId;
    private final UUID sellerId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final BigDecimal minIncrement;

    private AuctionStatus status = AuctionStatus.OPEN;
    private BigDecimal currentPrice;
    private UUID leadingBidderId;              // null khi chưa có bid
    private final List<BidTransaction> bids = new ArrayList<>();
    private final List<AutoBid> autoBids = new ArrayList<>();

    // ReentrantLock tự Serializable; đọc ra luôn ở trạng thái chưa khoá.
    private final ReentrantLock lock = new ReentrantLock();
    // Đọc nhiều (mỗi bid duyệt 1 lần), ghi ít (client vào/ra phiên) nên không cần lock riêng.
    // transient: listener là ClientHandler giữ socket, không lưu được; đọc ra thì tạo list rỗng (readObject).
    private transient List<AuctionListener> listeners = new CopyOnWriteArrayList<>();

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
        List<BidTransaction> placedBids = new ArrayList<>();

        BidTransaction manualBid;

        lock.lock();
        try {
            // Ghi nhận lượt đặt giá thủ công
            manualBid = recordBidLocked(bidder, amount);
            placedBids.add(manualBid);

            // Xử lý Auto-Bidding
            BidTransaction autoBid = processAutoBidsLocked();

            if (autoBid != null) {
                placedBids.add(autoBid);
            }

        } finally {
            lock.unlock();
        }

        // Thông báo Observer sau khi giải phóng lock
        for (BidTransaction bid : placedBids) {
            notifyBidPlaced(bid);
        }

        return manualBid;
    }

    private BidTransaction recordBidLocked(
            Bidder bidder,
            BigDecimal amount
    ) {
        Objects.requireNonNull(bidder, "bidder must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        LocalDateTime now = LocalDateTime.now();

        if (status != AuctionStatus.RUNNING
                || now.isBefore(startTime)
                || !now.isBefore(endTime)) {
            throw new AuctionClosedException(
                    "Phiên đấu giá không hoạt động"
            );
        }

        if (!bidder.canBid()) {
            throw new InvalidBidException(
                    "Bidder không có quyền đặt giá"
            );
        }

        BigDecimal minAccepted = currentPrice.add(minIncrement);

        if (amount.compareTo(minAccepted) < 0) {
            throw new InvalidBidException(
                    "Giá đặt phải lớn hơn hoặc bằng " + minAccepted
            );
        }

        BidTransaction bid = new BidTransaction(
                getId(),
                bidder.getId(),
                amount,
                now
        );

        bids.add(bid);
        currentPrice = amount;
        leadingBidderId = bidder.getId();

        // Anti-sniping
        Duration remainingTime = Duration.between(now, endTime);

        if (remainingTime.compareTo(ANTI_SNIPING_WINDOW) <= 0) {
            endTime = endTime.plus(ANTI_SNIPING_EXTENSION);

            LOG.log(
                    System.Logger.Level.INFO,
                    "Phiên {0} được gia hạn đến {1}",
                    getId(),
                    endTime
            );
        }

        markUpdated();

        return bid;
    }
    private BidTransaction processAutoBidsLocked() {
        if (autoBids.isEmpty()) {
            return null;
        }

        List<AutoBid> rankedAutoBids = new ArrayList<>(autoBids);

        rankedAutoBids.sort(
                Comparator.comparing(AutoBid::maxAmount)
                        .reversed()
                        .thenComparing(AutoBid::registeredAt)
        );

        AutoBid winner = rankedAutoBids.get(0);

        boolean winnerIsLeading = winner.bidder().getId()
                .equals(leadingBidderId);

        BigDecimal minAccepted = currentPrice.add(minIncrement);

        // Tìm mức tối đa cao nhất của các Auto-Bidder còn lại
        BigDecimal competingMax = currentPrice;

        for (int i = 1; i < rankedAutoBids.size(); i++) {
            BigDecimal maxAmount = rankedAutoBids.get(i).maxAmount();

            if (maxAmount.compareTo(competingMax) > 0) {
                competingMax = maxAmount;
            }
        }

        // Nếu người có mức tối đa cao nhất đang dẫn đầu,
        // chỉ tăng giá khi đối thủ đủ khả năng cạnh tranh.
        if (winnerIsLeading
                && competingMax.compareTo(minAccepted) < 0) {
            return null;
        }

        // Đặt giá cao hơn đối thủ mạnh nhất một bước giá
        BigDecimal targetAmount = competingMax.add(minIncrement);

        // Không vượt quá mức tối đa đã đăng ký
        if (targetAmount.compareTo(winner.maxAmount()) > 0) {
            targetAmount = winner.maxAmount();
        }

        if (targetAmount.compareTo(minAccepted) < 0) {
            return null;
        }

        return recordBidLocked(winner.bidder(), targetAmount);
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

    /**
     * Người thắng trả tiền: trừ currentPrice khỏi số dư rồi chuyển FINISHED -> PAID.
     * Kiểm tra, trừ tiền và đổi trạng thái nằm trong cùng một lock: hai request trả tiền đến cùng lúc
     * thì request sau thấy PAID và bị từ chối, không bị trừ tiền hai lần.
     * Không đủ tiền thì withdraw ném IllegalStateException trước khi đổi trạng thái, phiên vẫn FINISHED.
     */
    public void markPaid(Bidder payer) {
        Objects.requireNonNull(payer, "payer must not be null");
        AuctionStatus old;
        lock.lock();
        try {
            if (status != AuctionStatus.FINISHED) {
                throw new IllegalStateException("Chỉ thanh toán được phiên đã kết thúc, phiên đang " + status);
            }
            if (!payer.getId().equals(leadingBidderId)) {
                throw new IllegalArgumentException("Chỉ người thắng mới được thanh toán");
            }
            payer.withdraw(currentPrice);
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
        lock.lock();
        try {
            return endTime;
        } finally {
            lock.unlock();
        }
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
    public void registerAutoBid(Bidder bidder, BigDecimal maxAmount) {
        Objects.requireNonNull(bidder, "bidder must not be null");
        Objects.requireNonNull(maxAmount, "maxAmount must not be null");

        BidTransaction autoBid = null;

        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();

            if (status != AuctionStatus.RUNNING
                    || now.isBefore(startTime)
                    || !now.isBefore(endTime)) {
                throw new AuctionClosedException(
                        "Phiên đấu giá không hoạt động"
                );
            }

            if (!bidder.canBid()) {
                throw new InvalidBidException(
                        "Bidder không có quyền Auto-Bid"
                );
            }

            BigDecimal minAccepted = currentPrice.add(minIncrement);

            if (maxAmount.compareTo(minAccepted) < 0) {
                throw new InvalidBidException(
                        "Giá tối đa phải >= " + minAccepted
                );
            }

            boolean alreadyRegistered = autoBids.stream()
                    .anyMatch(existing ->
                            existing.bidder().getId().equals(bidder.getId())
                    );

            if (alreadyRegistered) {
                throw new InvalidBidException(
                        "Bidder đã đăng ký Auto-Bid"
                );
            }

            autoBids.add(new AutoBid(
                    bidder,
                    maxAmount,
                    LocalDateTime.now()
            ));

            // Chỉ kích hoạt ngay nếu đã có người dẫn đầu
            if (leadingBidderId != null) {
                autoBid = processAutoBidsLocked();
            }

        } finally {
            lock.unlock();
        }

        // Không gọi Observer khi vẫn đang giữ lock
        if (autoBid != null) {
            notifyBidPlaced(autoBid);
        }
    }

    // ============== SERIALIZATION ==============

    // Ghi trong lock: nếu đang ghi file mà thread khác placeBid thì list bids bị sửa giữa chừng
    // (ConcurrentModificationException hoặc file lưu nửa cũ nửa mới).
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        lock.lock();
        try {
            out.defaultWriteObject();
        } finally {
            lock.unlock();
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        listeners = new CopyOnWriteArrayList<>();
    }
}
