package com.auction.model.entity;

import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemCategory;
import com.auction.model.enums.ItemCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.auction.exception.InvalidBidException;
import java.util.concurrent.atomic.AtomicInteger;
import com.auction.model.observer.AuctionListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        UUID sellerId = UUID.randomUUID();

        Item item = new OtherItem(
                "Laptop",
                "Laptop test",
                sellerId,
                new BigDecimal("10000000"),
                List.of(),
                ItemCategory.OTHER,
                ItemCondition.NEW,
                "Test item"
        );

        LocalDateTime now = LocalDateTime.now();

        auction = new Auction(
                item,
                now.minusMinutes(1),
                now.plusMinutes(10),
                new BigDecimal("100000")
        );
    }

    @Test
    void startIfDue_shouldStartAuction_whenStartTimeHasPassed() {
        // Act
        boolean result = auction.startIfDue();

        // Assert
        assertTrue(result);
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void startIfDue_shouldNotStartAuction_whenStartTimeIsInFuture() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        Item item = new OtherItem(
                "Phone",
                "Phone test",
                UUID.randomUUID(),
                new BigDecimal("5000000"),
                List.of(),
                ItemCategory.OTHER,
                ItemCondition.NEW,
                "Test item"
        );

        Auction futureAuction = new Auction(
                item,
                now.plusMinutes(10),
                now.plusMinutes(20),
                new BigDecimal("100000")
        );

        // Act
        boolean result = futureAuction.startIfDue();

        // Assert
        assertFalse(result);
        assertEquals(AuctionStatus.OPEN, futureAuction.getStatus());
    }

    @Test
    void startIfDue_shouldNotStartAgain_whenAuctionIsAlreadyRunning() {
        // Arrange
        auction.startIfDue();

        // Act
        boolean result = auction.startIfDue();

        // Assert
        assertFalse(result);
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }
    @Test
    void placeBid_shouldUpdateAuction_whenBidIsValid() {
        // Arrange
        auction.startIfDue();

        Bidder bidder = new Bidder(
                "alice",
                "passwordHash",
                "alice@example.com"
        );

        BigDecimal amount = new BigDecimal("10100000");

        // Act
        BidTransaction bid = auction.placeBid(bidder, amount);

        // Assert
        assertEquals(amount, auction.getCurrentPrice());
        assertEquals(bidder.getId(), auction.getLeadingBidderId());
        assertEquals(1, auction.getBids().size());
        assertEquals(amount, bid.amount());
    }
    @Test
    void placeBid_shouldThrowException_whenBidIsTooLow() {
        // Arrange
        auction.startIfDue();

        Bidder bidder = new Bidder(
                "bob",
                "passwordHash",
                "bob@example.com"
        );

        BigDecimal invalidAmount = new BigDecimal("10050000");

        // Act & Assert
        assertThrows(
                InvalidBidException.class,
                () -> auction.placeBid(bidder, invalidAmount)
        );

        // Verify auction state remains unchanged
        assertEquals(new BigDecimal("10000000"), auction.getCurrentPrice());
        assertTrue(auction.getBids().isEmpty());
    }
    @Test
    void finishIfExpired_shouldCancelAuction_whenThereAreNoBids() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        Item item = new OtherItem(
                "Old Laptop",
                "Expired auction test",
                UUID.randomUUID(),
                new BigDecimal("10000000"),
                List.of(),
                ItemCategory.OTHER,
                ItemCondition.NEW,
                "Test item"
        );

        Auction expiredAuction = new Auction(
                item,
                now.minusMinutes(10),
                now.minusMinutes(1),
                new BigDecimal("100000")
        );

        // Act
        expiredAuction.startIfDue();
        boolean result = expiredAuction.finishIfExpired();

        // Assert
        assertTrue(result);
        assertEquals(AuctionStatus.CANCELED, expiredAuction.getStatus());
        assertTrue(expiredAuction.getBids().isEmpty());
    }
    @Test
    void finish_shouldFinishAuction_whenThereAreBids() {
        // Arrange
        auction.startIfDue();

        Bidder bidder = new Bidder(
                "charlie",
                "passwordHash",
                "charlie@example.com"
        );

        auction.placeBid(bidder, new BigDecimal("10100000"));

        // Act
        auction.finish();

        // Assert
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(1, auction.getBids().size());
        assertEquals(bidder.getId(), auction.getLeadingBidderId());
    }
    @Test
    void placeBid_shouldNotifyListener_whenBidIsPlaced() {
        // Arrange
        auction.startIfDue();

        AtomicInteger notificationCount = new AtomicInteger(0);

        auction.addListener(new AuctionListener() {
            @Override
            public void onBidPlaced(Auction auction, BidTransaction bid) {
                notificationCount.incrementAndGet();
            }

            @Override
            public void onStatusChanged(
                    Auction auction,
                    AuctionStatus oldStatus,
                    AuctionStatus newStatus
            ) {
            }
        });

        Bidder bidder = new Bidder(
                "david",
                "passwordHash",
                "david@example.com"
        );

        // Act
        auction.placeBid(bidder, new BigDecimal("10100000"));

        // Assert
        assertEquals(1, notificationCount.get());
    }
    @Test
    void startIfDue_shouldNotifyListener_whenStatusChanges() {
        // Arrange
        AtomicInteger notificationCount = new AtomicInteger(0);

        auction.addListener(new AuctionListener() {
            @Override
            public void onBidPlaced(Auction auction, BidTransaction bid) {
            }

            @Override
            public void onStatusChanged(
                    Auction auction,
                    AuctionStatus oldStatus,
                    AuctionStatus newStatus
            ) {
                assertEquals(AuctionStatus.OPEN, oldStatus);
                assertEquals(AuctionStatus.RUNNING, newStatus);

                notificationCount.incrementAndGet();
            }
        });

        // Act
        auction.startIfDue();

        // Assert
        assertEquals(1, notificationCount.get());
    }
    @Test
    void placeBid_shouldNotifyOtherListeners_whenOneListenerThrowsException() {
        // Arrange
        auction.startIfDue();

        AtomicInteger successfulListenerCalls = new AtomicInteger(0);

        // Listener thứ nhất cố tình gây lỗi
        auction.addListener(new AuctionListener() {
            @Override
            public void onBidPlaced(Auction auction, BidTransaction bid) {
                throw new RuntimeException("Listener failed");
            }

            @Override
            public void onStatusChanged(
                    Auction auction,
                    AuctionStatus oldStatus,
                    AuctionStatus newStatus
            ) {
            }
        });

        // Listener thứ hai vẫn phải nhận được thông báo
        auction.addListener(new AuctionListener() {
            @Override
            public void onBidPlaced(Auction auction, BidTransaction bid) {
                successfulListenerCalls.incrementAndGet();
            }

            @Override
            public void onStatusChanged(
                    Auction auction,
                    AuctionStatus oldStatus,
                    AuctionStatus newStatus
            ) {
            }
        });

        Bidder bidder = new Bidder(
                "listener_test",
                "passwordHash",
                "listener_test@example.com"
        );

        BigDecimal amount = new BigDecimal("10100000");

        // Act
        assertDoesNotThrow(() -> auction.placeBid(bidder, amount));

        // Assert
        assertEquals(1, successfulListenerCalls.get());
        assertEquals(amount, auction.getCurrentPrice());
        assertEquals(1, auction.getBids().size());
    }
    
@Test
    void placeBid_shouldHandleConcurrentBidsCorrectly() throws Exception {
        // Arrange
        auction.startIfDue();

        Bidder bidder1 = new Bidder(
                "concurrent_alice",
                "passwordHash",
                "concurrent_alice@example.com"
        );

        Bidder bidder2 = new Bidder(
                "concurrent_bob",
                "passwordHash",
                "concurrent_bob@example.com"
        );

        BigDecimal amount = new BigDecimal("10100000");

        AtomicInteger acceptedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            // Act: Thread 1
            Future<?> future1 = executor.submit(() -> {
                ready.countDown();

                if (!startSignal.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout waiting to start");
                }

                try {
                    auction.placeBid(bidder1, amount);
                    acceptedCount.incrementAndGet();
                } catch (InvalidBidException ignored) {
                    // Bid bị từ chối là kết quả hợp lệ của kịch bản này
                }

                return null;
            });

            // Act: Thread 2
            Future<?> future2 = executor.submit(() -> {
                ready.countDown();

                if (!startSignal.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout waiting to start");
                }

                try {
                    auction.placeBid(bidder2, amount);
                    acceptedCount.incrementAndGet();
                } catch (InvalidBidException ignored) {
                    // Bid bị từ chối là kết quả hợp lệ của kịch bản này
                }

                return null;
            });

            // Chờ cả hai thread sẵn sàng
            assertTrue(ready.await(5, TimeUnit.SECONDS));

            // Cho hai thread bắt đầu gần như đồng thời
            startSignal.countDown();

            // Chờ hai thread hoàn thành
            future1.get(5, TimeUnit.SECONDS);
            future2.get(5, TimeUnit.SECONDS);

        } finally {
            startSignal.countDown();
            executor.shutdownNow();
        }

        // Assert
        assertEquals(1, acceptedCount.get());

        assertEquals(amount, auction.getCurrentPrice());

        assertEquals(1, auction.getBids().size());

        assertTrue(
                auction.getLeadingBidderId().equals(bidder1.getId())
                        || auction.getLeadingBidderId().equals(bidder2.getId())
        );
    }
}
