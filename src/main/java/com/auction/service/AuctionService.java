package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.model.entity.Auction;
import com.auction.model.entity.BidTransaction;
import com.auction.model.entity.Bidder;
import com.auction.model.entity.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Cửa vào nghiệp vụ đấu giá cho server (B): nhận id, tìm phiên qua AuctionDao rồi gọi xuống Auction.
 *
 * Không tự lock: lock và kiểm tra giá nằm trong Auction.placeBid, vì:
 * - Lock phải nằm cạnh dữ liệu nó bảo vệ. currentPrice, status, bids là của Auction; nếu lock đặt ở đây
 *   thì nơi nào gọi thẳng auction.placeBid() (AuctionScheduler, test, service khác) sẽ lọt qua lock.
 * - Mỗi phiên một lock riêng: đặt giá ở hai phiên khác nhau chạy song song được. Một lock chung
 *   ở service sẽ bắt mọi phiên xếp hàng chung.
 * - Kiểm tra "giá >= giá hiện tại + bước giá" và ghi giá mới phải trong cùng một lock; finish() của
 *   Scheduler cũng dùng lock đó, nên một bid không thể chen vào đúng lúc phiên đang đóng.
 * - Thêm một lock nữa ở đây là thừa, và lock lồng nhau dễ gây deadlock.
 */
public class AuctionService {
    private final AuctionDao dao;

    /** Mặc định lưu trong RAM. Giữ constructor này để server của B (new AuctionService()) không phải sửa. */
    public AuctionService() {
        this(AuctionManager.getInstance());
    }

    /** Server truyền FileAuctionDao vào đây để lưu ra file. */
    public AuctionService(AuctionDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
    }

    public Auction createAuction(Item item, LocalDateTime start, LocalDateTime end, BigDecimal increment) {
        Auction auction = new Auction(item, start, end, increment);
        dao.save(auction);
        return auction;
    }

    /**
     * Ném AuctionClosedException / InvalidBidException từ Auction, B bắt và trả lỗi về client.
     * Bid xong thì save lại để bản lưu file có giá mới.
     */
    public BidTransaction placeBid(UUID auctionId, Bidder bidder, BigDecimal amount) {
        Auction auction = getAuction(auctionId);
        BidTransaction bid = auction.placeBid(bidder, amount);
        dao.save(auction);
        return bid;
    }

    public Auction getAuction(UUID auctionId) {
        return dao.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không có phiên " + auctionId));
    }

    public List<Auction> listAuctions() {
        return dao.findAll();
    }
}
