package com.auction.model.observer;

import com.auction.model.entity.Auction;
import com.auction.model.entity.BidTransaction;
import com.auction.model.enums.AuctionStatus;

/**
 * Nhận sự kiện từ một phiên Auction (Observer).
 * Được gọi sau khi Auction đã nhả lock, trên chính thread vừa đặt giá / đổi trạng thái,
 * nên listener không được chặn lâu (B broadcast nên đẩy sang thread khác nếu gửi mạng chậm).
 */
public interface AuctionListener {
    void onBidPlaced(Auction auction, BidTransaction bid);

    void onStatusChanged(Auction auction, AuctionStatus oldStatus, AuctionStatus newStatus);
}
