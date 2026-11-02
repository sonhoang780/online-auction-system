package com.auction.service;

import com.auction.model.entity.Auction;
import com.auction.model.entity.BidTransaction;
import com.auction.model.entity.Bidder;
import com.auction.model.entity.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Cửa vào nghiệp vụ đấu giá cho server (B): nhận id, tìm phiên trong AuctionManager rồi gọi xuống Auction.
 * Không tự lock: lock và kiểm tra giá nằm trong Auction.placeBid.
 */
public class AuctionService {
    private final AuctionManager manager = AuctionManager.getInstance();

    public Auction createAuction(Item item, LocalDateTime start, LocalDateTime end, BigDecimal increment) {
        Auction auction = new Auction(item, start, end, increment);
        manager.add(auction);
        return auction;
    }

    /**
     * Ném AuctionClosedException / InvalidBidException từ Auction, B bắt và trả lỗi về client.
     */
    public BidTransaction placeBid(UUID auctionId, Bidder bidder, BigDecimal amount) {
        return getAuction(auctionId).placeBid(bidder, amount);
    }

    public Auction getAuction(UUID auctionId) {
        return manager.get(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không có phiên " + auctionId));
    }

    public List<Auction> listAuctions() {
        return manager.findAll();
    }
}
