package com.auction.service;

import com.auction.dao.FileAuctionDao;
import com.auction.dao.FileUserDao;
import com.auction.dao.AuctionDao;
import com.auction.model.entity.Auction;
import com.auction.model.entity.Bidder;
import com.auction.model.entity.OtherItem;
import com.auction.model.entity.Seller;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemCategory;
import com.auction.model.enums.ItemCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionServicePayTest {

    @TempDir
    Path dir;

    private AuctionService service;
    private FileUserDao users;
    private Auction auction;
    private Bidder winner;
    private Bidder loser;

    /** Bộ nhớ riêng cho từng test; AuctionManager là singleton nên dùng chung sẽ lẫn dữ liệu giữa các test. */
    private static class MapDao implements AuctionDao {
        private final Map<UUID, Auction> map = new ConcurrentHashMap<>();
        public void save(Auction a) { map.put(a.getId(), a); }
        public Optional<Auction> findById(UUID id) { return Optional.ofNullable(map.get(id)); }
        public List<Auction> findAll() { return List.copyOf(map.values()); }
    }

    /** Phiên giá khởi điểm 1000, winner đặt 1500 rồi phiên kết thúc (FINISHED). */
    @BeforeEach
    void setUp() {
        users = new FileUserDao(dir.resolve("users.dat"));
        service = new AuctionService(new FileAuctionDao(dir.resolve("auctions.dat"), new MapDao()), users);

        winner = new Bidder("an", "hash", "an@example.com");
        loser = new Bidder("binh", "hash", "binh@example.com");
        winner.deposit(new BigDecimal("2000"));
        loser.deposit(new BigDecimal("2000"));
        users.save(winner);
        users.save(loser);

        OtherItem item = new OtherItem("Laptop", "test", UUID.randomUUID(), new BigDecimal("1000"),
                List.of(), ItemCategory.OTHER, ItemCondition.NEW, "x");
        LocalDateTime now = LocalDateTime.now();
        auction = service.createAuction(item, now.minusMinutes(1), now.plusMinutes(10), new BigDecimal("100"));
        auction.start();
        service.placeBid(auction.getId(), winner, new BigDecimal("1500"));
        auction.finish();
    }

    @Test
    void winnerPays_balanceDeductedStatusPaid_andSavedToFile() {
        service.payAuction(auction.getId(), winner.getId());

        assertEquals(AuctionStatus.PAID, auction.getStatus());
        assertEquals(0, new BigDecimal("500").compareTo(winner.getBalance()));

        // Đọc lại từ file: cả số dư lẫn trạng thái đều đã được lưu.
        Bidder reloaded = (Bidder) new FileUserDao(dir.resolve("users.dat")).findById(winner.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("500").compareTo(reloaded.getBalance()));
        Auction reloadedAuction = new FileAuctionDao(dir.resolve("auctions.dat"), new MapDao())
                .findById(auction.getId()).orElseThrow();
        assertEquals(AuctionStatus.PAID, reloadedAuction.getStatus());
    }

    @Test
    void notWinner_rejectedAndNothingChanges() {
        assertThrows(IllegalArgumentException.class, () -> service.payAuction(auction.getId(), loser.getId()));

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(0, new BigDecimal("2000").compareTo(loser.getBalance()));
    }

    @Test
    void notEnoughMoney_rejectedAndStaysFinished() {
        winner.withdraw(new BigDecimal("1000")); // còn 1000 < 1500

        assertThrows(IllegalStateException.class, () -> service.payAuction(auction.getId(), winner.getId()));

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(0, new BigDecimal("1000").compareTo(winner.getBalance()));
    }

    @Test
    void sellerCannotPay() {
        Seller seller = new Seller("chi", "hash", "chi@example.com");
        users.save(seller);

        assertThrows(IllegalArgumentException.class, () -> service.payAuction(auction.getId(), seller.getId()));
    }

    @Test
    void payTwiceConcurrently_chargedOnlyOnce() throws Exception {
        // Đủ tiền trả nhiều lần: nếu bị trừ 2 lần thì số dư lộ ra ngay, không bị "thiếu tiền" che mất.
        winner.deposit(new BigDecimal("8000")); // 10000
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            results.add(pool.submit(() -> {
                go.await();
                try {
                    service.payAuction(auction.getId(), winner.getId());
                    return true;
                } catch (IllegalStateException e) {
                    return false; // đã PAID
                }
            }));
        }
        go.countDown();
        int paid = 0;
        for (Future<Boolean> f : results) {
            if (f.get(5, TimeUnit.SECONDS)) paid++;
        }
        pool.shutdown();

        assertEquals(1, paid);
        assertEquals(0, new BigDecimal("8500").compareTo(winner.getBalance()));
    }
}
