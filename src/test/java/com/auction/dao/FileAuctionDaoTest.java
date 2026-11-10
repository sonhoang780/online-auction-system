package com.auction.dao;

import com.auction.model.entity.Auction;
import com.auction.model.entity.BidTransaction;
import com.auction.model.entity.Bidder;
import com.auction.model.entity.OtherItem;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemCategory;
import com.auction.model.enums.ItemCondition;
import com.auction.model.observer.AuctionListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FileAuctionDaoTest {

    @TempDir
    Path dir;

    /** Bộ nhớ riêng cho từng test; AuctionManager là singleton nên dùng chung sẽ lẫn dữ liệu giữa các test. */
    private static class MapDao implements AuctionDao {
        private final Map<UUID, Auction> map = new HashMap<>();
        public void save(Auction a) { map.put(a.getId(), a); }
        public Optional<Auction> findById(UUID id) { return Optional.ofNullable(map.get(id)); }
        public List<Auction> findAll() { return List.copyOf(map.values()); }
    }

    private static Auction newRunningAuction() {
        OtherItem item = new OtherItem("Laptop", "test", UUID.randomUUID(), new BigDecimal("1000"),
                List.of(), ItemCategory.OTHER, ItemCondition.NEW, "x");
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction(item, now.minusMinutes(1), now.plusMinutes(10), new BigDecimal("100"));
        auction.startIfDue();
        return auction;
    }

    @Test
    void saveThenReload_keepsIdPriceBidsAndStatus() {
        Path file = dir.resolve("data/auctions.dat");
        Auction auction = newRunningAuction();
        Bidder bidder = new Bidder("an", "hash", "an@example.com");
        auction.placeBid(bidder, new BigDecimal("1500"));

        new FileAuctionDao(file, new MapDao()).save(auction);

        // Server khởi động lại: bộ nhớ mới, chỉ còn file.
        Auction loaded = new FileAuctionDao(file, new MapDao()).findById(auction.getId()).orElseThrow();

        assertNotSame(auction, loaded);
        assertEquals(auction.getId(), loaded.getId());
        assertEquals(auction.getCreatedAt(), loaded.getCreatedAt());
        assertEquals(0, new BigDecimal("1500").compareTo(loaded.getCurrentPrice()));
        assertEquals(bidder.getId(), loaded.getLeadingBidderId());
        assertEquals(1, loaded.getBids().size());
        assertEquals(AuctionStatus.RUNNING, loaded.getStatus());
    }

    @Test
    void reloadedAuction_lockAndListenersStillWork() {
        Path file = dir.resolve("auctions.dat");
        new FileAuctionDao(file, new MapDao()).save(newRunningAuction());
        Auction loaded = new FileAuctionDao(file, new MapDao()).findAll().get(0);

        // listeners là transient: nếu readObject không tạo lại list thì addListener ném NullPointerException.
        AtomicInteger notified = new AtomicInteger();
        loaded.addListener(new AuctionListener() {
            public void onBidPlaced(Auction a, BidTransaction b) { notified.incrementAndGet(); }
            public void onStatusChanged(Auction a, AuctionStatus o, AuctionStatus n) { }
        });
        // placeBid lấy lock: nếu lock đọc ra ở trạng thái khoá thì dòng này treo.
        loaded.placeBid(new Bidder("binh", "hash", "binh@example.com"), new BigDecimal("2000"));

        assertEquals(1, notified.get());
    }

    @Test
    void corruptFile_throwsInsteadOfStartingEmpty() throws IOException {
        Path file = dir.resolve("auctions.dat");
        Files.writeString(file, "không phải dữ liệu serialization");

        assertThrows(IllegalStateException.class, () -> new FileAuctionDao(file, new MapDao()));
    }
}
