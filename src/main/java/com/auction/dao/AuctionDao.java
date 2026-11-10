package com.auction.dao;

import com.auction.model.entity.Auction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Nơi lưu phiên đấu giá. Service chỉ biết interface này, không biết dữ liệu nằm ở RAM hay file.
 * - AuctionManager: lưu trong RAM (mất khi tắt server)
 * - FileAuctionDao: lưu ra file bằng Serialization
 */
public interface AuctionDao {
    /** Thêm mới hoặc ghi đè phiên cùng id. */
    void save(Auction auction);

    Optional<Auction> findById(UUID id);

    List<Auction> findAll();
}
