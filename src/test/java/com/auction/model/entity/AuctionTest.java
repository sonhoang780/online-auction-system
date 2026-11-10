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
import com.auction.exception.AuctionClosedException;

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
    private Bidder createBidder(String username) {
        return new Bidder(
                username,
                "hashed-password",
                username + "@example.com"
        );
    }

    private void startAuction() {
        auction.startIfDue();
    }
    // === KIỂM THỬ VÒNG ĐỜI ===
     
    //1.Bắt đầu phiên đấu giá khi đã đến thời điểm
    @Test
    void startIfDue_shouldStartAuction_whenStartTimeHasPassed() {
        // Act
        boolean result = auction.startIfDue();

        // Assert
        assertTrue(result);
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }
    
    //2.Không bắt đầu phiên đấu giá khi chưa đến thời điểm
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

    //3.Không bắt đầu lại khi phiên đang chạy
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

    //4.Hủy phiên đấu giá hết hạn nhưng không có bid
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

    //5.Kết thúc phiên đấu giá khi đã có bid
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

    // === KIỂM THỬ BIDDING ===

    //6.Đặt giá hợp lệ
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
    
    //7.Từ chối giá đặt quá thấp
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

    //8.Không cho đặt giá khi phiên chưa bắt đầu
    @Test
    void placeBid_shouldThrowException_whenAuctionHasNotStarted() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        Item item = new OtherItem(
                "Future Laptop",
                "Future auction test",
                UUID.randomUUID(),
                new BigDecimal("10000000"),
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

        Bidder bidder = new Bidder(
                "future_bidder",
                "passwordHash",
                "future@example.com"
        );

        // Act & Assert
        assertThrows(
                AuctionClosedException.class,
                () -> futureAuction.placeBid(
                        bidder,
                        new BigDecimal("10100000")
                )
        );

        // Verify state remains unchanged
        assertEquals(AuctionStatus.OPEN, futureAuction.getStatus());
        assertEquals(new BigDecimal("10000000"), futureAuction.getCurrentPrice());
        assertTrue(futureAuction.getBids().isEmpty());
    }

    //9.Không cho đặt giá khi phiên đã hết hạn
    @Test
    void placeBid_shouldThrowException_whenAuctionHasExpired() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        Item item = new OtherItem(
                "Expired Laptop",
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

        expiredAuction.startIfDue();

        Bidder bidder = new Bidder(
                "expired_bidder",
                "passwordHash",
                "expired@example.com"
        );

        // Act & Assert
        assertThrows(
                AuctionClosedException.class,
                () -> expiredAuction.placeBid(
                        bidder,
                        new BigDecimal("10100000")
                )
        );

        // Verify state remains unchanged
        assertEquals(AuctionStatus.RUNNING, expiredAuction.getStatus());
        assertEquals(new BigDecimal("10000000"), expiredAuction.getCurrentPrice());
        assertTrue(expiredAuction.getBids().isEmpty());
    }

    //10.Không cho đặt giá khi Bidder là null
    @Test
    void placeBid_shouldThrowException_whenBidderIsNull() {
        // Arrange
        auction.startIfDue();

        // Act & Assert
        assertThrows(
                NullPointerException.class,
                () -> auction.placeBid(
                        null,
                        new BigDecimal("10100000")
                )
        );

        // Verify state remains unchanged
        assertEquals(new BigDecimal("10000000"), auction.getCurrentPrice());
        assertTrue(auction.getBids().isEmpty());
    }

    //11.Không cho đặt giá khi Amount là null
    @Test
    void placeBid_shouldThrowException_whenAmountIsNull() {
        // Arrange
        auction.startIfDue();

        Bidder bidder = new Bidder(
                "null_amount_bidder",
                "passwordHash",
                "nullamount@example.com"
        );

        // Act & Assert
        assertThrows(
                NullPointerException.class,
                () -> auction.placeBid(bidder, null)
        );

        // Verify state remains unchanged
        assertEquals(new BigDecimal("10000000"), auction.getCurrentPrice());
        assertTrue(auction.getBids().isEmpty());
    }

    //12.Không cho Bidder không đủ quyền đặt giá
    @Test
    void placeBid_shouldThrowException_whenBidderCannotBid() {
        // Arrange
        auction.startIfDue();

        Bidder unauthorizedBidder = new Bidder(
                "unauthorized",
                "passwordHash",
                "unauthorized@example.com"
        ) {
            @Override
            public boolean canBid() {
                return false;
            }
        };

        // Act & Assert
        assertThrows(
                InvalidBidException.class,
                () -> auction.placeBid(
                        unauthorizedBidder,
                        new BigDecimal("10100000")
                )
        );

        // Verify state remains unchanged
        assertEquals(new BigDecimal("10000000"), auction.getCurrentPrice());
        assertTrue(auction.getBids().isEmpty());
    }

    //=== KIỂM THỬ CONCURRENCY ===

    //13.Hai người đặt giá đồng thời
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

    // === KIỂM THỬ OBSERVER/LISTENER

    //14.Thông báo Listener khi có bid mới
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

    //15.Thông báo Listener khi trạng thái phiên thay đổi
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

    //16.Một Listener bị lỗi nhưng các Listener khác vẫn hoạt động
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

    // === KIỂM THỬ ANTI-SNIPING ===

    //17.Gia hạn phiên khi có bid trong 30 giây cuối
    @Test
    void placeBid_shouldExtendEndTime_whenBidIsPlacedWithinLast30Seconds() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        Item item = new OtherItem(
                "Anti-sniping Laptop",
                "Anti-sniping test",
                UUID.randomUUID(),
                new BigDecimal("10000000"),
                List.of(),
                ItemCategory.OTHER,
                ItemCondition.NEW,
                "Test item"
        );

        Auction testAuction = new Auction(
                item,
                now.minusMinutes(1),
                now.plusSeconds(20),
                new BigDecimal("100000")
        );

        testAuction.startIfDue();

        LocalDateTime originalEndTime = testAuction.getEndTime();

        Bidder bidder = new Bidder(
                "anti_sniping_bidder",
                "passwordHash",
                "anti_sniping@example.com"
        );

        // Act
        testAuction.placeBid(bidder, new BigDecimal("10100000"));

        // Assert
        assertEquals(
                originalEndTime.plusSeconds(30),
                testAuction.getEndTime()
        );
    }

    //18.Không gia hạn khi còn hơn 30 giây
    @Test
    void placeBid_shouldNotExtendEndTime_whenMoreThan30SecondsRemain() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        Item item = new OtherItem(
                "Normal Laptop",
                "Normal auction test",
                UUID.randomUUID(),
                new BigDecimal("10000000"),
                List.of(),
                ItemCategory.OTHER,
                ItemCondition.NEW,
                "Test item"
        );

        Auction testAuction = new Auction(
                item,
                now.minusMinutes(1),
                now.plusMinutes(2),
                new BigDecimal("100000")
        );

        testAuction.startIfDue();

        LocalDateTime originalEndTime = testAuction.getEndTime();

        Bidder bidder = new Bidder(
                "normal_bidder",
                "passwordHash",
                "normal@example.com"
        );

        // Act
        testAuction.placeBid(bidder, new BigDecimal("10100000"));

        // Assert
        assertEquals(originalEndTime, testAuction.getEndTime());
    }

    // === KIỂM THỬ AUTO-BIDDING ===

    //19. Auto-bid tự động khi người đăng kí bị vượt giá
    @Test
    void placeBid_shouldTriggerAutoBid_whenRegisteredBidderIsOutbid() {
        startAuction();

        Bidder autoBidder = createBidder("alice");
        Bidder manualBidder = createBidder("bob");

        auction.registerAutoBid(
                autoBidder,
                new BigDecimal("12000000")
        );

        auction.placeBid(
                manualBidder,
                new BigDecimal("10200000")
        );

        assertEquals(
                autoBidder.getId(),
                auction.getLeadingBidderId()
        );

        assertEquals(
                new BigDecimal("10300000"),
                auction.getCurrentPrice()
        );
    }

    //20. Không kích hoạt Auto-Bid khi giá thủ công vượt mức tối đa
    @Test
    void placeBid_shouldNotTriggerAutoBid_whenManualBidExceedsMaxAmount() {
        startAuction();

        Bidder autoBidder = createBidder("alice");
        Bidder manualBidder = createBidder("bob");

        auction.registerAutoBid(
                autoBidder,
                new BigDecimal("11000000")
        );

        auction.placeBid(
                manualBidder,
                new BigDecimal("11500000")
        );

        assertEquals(
                manualBidder.getId(),
                auction.getLeadingBidderId()
        );

        assertEquals(
                new BigDecimal("11500000"),
                auction.getCurrentPrice()
        );
    }

    //21.Chọn người có mức giá tối đa cao nhất
    @Test
    void placeBid_shouldSelectHighestMaxAutoBidder() {
        startAuction();

        Bidder bidderA = createBidder("alice");
        Bidder bidderB = createBidder("bob");
        Bidder manualBidder = createBidder("charlie");

        auction.registerAutoBid(
                bidderA,
                new BigDecimal("12000000")
        );

        auction.registerAutoBid(
                bidderB,
                new BigDecimal("13000000")
        );

        auction.placeBid(
                manualBidder,
                new BigDecimal("10200000")
        );

        assertEquals(
                bidderB.getId(),
                auction.getLeadingBidderId()
        );

        assertEquals(
                new BigDecimal("12100000"),
                auction.getCurrentPrice()
        );
    }

    //22.Ưu tiên người đăng kí trước khi mức giá tối đa bằng nhau
    @Test
    void placeBid_shouldPrioritizeEarlierRegistration_whenMaxAmountsAreEqual() {
        startAuction();

        Bidder firstBidder = createBidder("alice");
        Bidder secondBidder = createBidder("bob");
        Bidder manualBidder = createBidder("charlie");

        BigDecimal maxAmount = new BigDecimal("12000000");

        auction.registerAutoBid(firstBidder, maxAmount);
        auction.registerAutoBid(secondBidder, maxAmount);

        auction.placeBid(
                manualBidder,
                new BigDecimal("10200000")
        );

        assertEquals(
                firstBidder.getId(),
                auction.getLeadingBidderId()
        );
    }

    //23.Đăng kí Auto-Bid khi đã có người dẫn đầu
    @Test
    void registerAutoBid_shouldTriggerAutomaticBid_whenAuctionAlreadyHasLeader() {
        startAuction();

        Bidder manualBidder = createBidder("alice");
        Bidder autoBidder = createBidder("bob");

        auction.placeBid(
                manualBidder,
                new BigDecimal("10500000")
        );

        auction.registerAutoBid(
                autoBidder,
                new BigDecimal("12000000")
        );

        assertEquals(
                autoBidder.getId(),
                auction.getLeadingBidderId()
        );

        assertEquals(
                new BigDecimal("10600000"),
                auction.getCurrentPrice()
        );
    }

    //24.Đặt giá cao hơn mức tối đa đứng thứ hai một bước giá
    @Test
    void placeBid_shouldBidJustAboveSecondHighestMax_whenMultipleAutoBiddersCompete() {
        startAuction();

        Bidder bidderA = createBidder("alice");
        Bidder bidderB = createBidder("bob");
        Bidder bidderC = createBidder("charlie");

        // A đăng ký tối đa 15 triệu
        auction.registerAutoBid(
                bidderA,
                new BigDecimal("15000000")
        );

        // B đăng ký tối đa 12 triệu
        auction.registerAutoBid(
                bidderB,
                new BigDecimal("12000000")
        );

        // C đặt giá thủ công 11 triệu
        auction.placeBid(
                bidderC,
                new BigDecimal("11000000")
        );

        // A phải tự động trả 12,1 triệu
        assertEquals(
                0,
                new BigDecimal("12100000")
                        .compareTo(auction.getCurrentPrice())
        );

        assertEquals(
                bidderA.getId(),
                auction.getLeadingBidderId()
        );

        assertEquals(2, auction.getBids().size());
    }

    //25.Người dẫn đầu phải nâng giá khi xuất hiện đối thủ Auto-Bid mới
    @Test
    void registerAutoBid_shouldRaiseCurrentBid_whenLeadingAutoBidderFacesNewCompetitor() {
        startAuction();

        Bidder bidderA = createBidder("alice");
        Bidder bidderB = createBidder("bob");
        Bidder bidderC = createBidder("charlie");

        // C đặt giá thủ công và dẫn đầu
        auction.placeBid(
                bidderC,
                new BigDecimal("11000000")
        );

        // A đăng ký Auto-Bid tối đa 15 triệu
        auction.registerAutoBid(
                bidderA,
                new BigDecimal("15000000")
        );

        // A trở thành người dẫn đầu với giá 11,1 triệu
        assertEquals(
                0,
                new BigDecimal("11100000")
                        .compareTo(auction.getCurrentPrice())
        );

        // B đăng ký Auto-Bid tối đa 12 triệu
        auction.registerAutoBid(
                bidderB,
                new BigDecimal("12000000")
        );

        // A phải nâng giá lên 12,1 triệu
        assertEquals(
                0,
                new BigDecimal("12100000")
                        .compareTo(auction.getCurrentPrice())
        );

        assertEquals(
                bidderA.getId(),
                auction.getLeadingBidderId()
        );
    }

    //26.Từ chối khi mức giá tối đa quá thấp
    @Test
    void registerAutoBid_shouldThrowException_whenMaxAmountIsTooLow() {
        startAuction();

        Bidder bidder = createBidder("alice");

        assertThrows(
                InvalidBidException.class,
                () -> auction.registerAutoBid(
                        bidder,
                        new BigDecimal("10050000")
                )
        );

        assertTrue(auction.getBids().isEmpty());
    }

    //27. Không cho một Bidder đăng kí Auto-Bid 2 lần
    @Test
    void registerAutoBid_shouldThrowException_whenBidderRegistersTwice() {
        startAuction();

        Bidder bidder = createBidder("alice");

        auction.registerAutoBid(
                bidder,
                new BigDecimal("12000000")
        );

        assertThrows(
                InvalidBidException.class,
                () -> auction.registerAutoBid(
                        bidder,
                        new BigDecimal("13000000")
                )
        );
    }

    //28.Không cho đăng kí Auto-Bid khi phiên chưa bắt đầu
    @Test
    void registerAutoBid_shouldThrowException_whenAuctionHasNotStarted() {
        Bidder bidder = createBidder("alice");

        assertThrows(
                AuctionClosedException.class,
                () -> auction.registerAutoBid(
                        bidder,
                        new BigDecimal("12000000")
                )
        );
    }

    //29.Không cho đăng kí Auto-Bid khi Bidder là null
    @Test
    void registerAutoBid_shouldThrowException_whenBidderIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> auction.registerAutoBid(
                        null,
                        new BigDecimal("12000000")
                )
        );
    }

    //30. Không cho đăng kí Auto-Bid khi MaxAmount là null
    @Test
    void registerAutoBid_shouldThrowException_whenMaxAmountIsNull() {
        startAuction();

        Bidder bidder = createBidder("alice");

        assertThrows(
                NullPointerException.class,
                () -> auction.registerAutoBid(bidder, null)
        );
    }

    //31. Không cho đăng kí Auto-Bid khi Bidder không đủ quyền
    @Test
    void registerAutoBid_shouldThrowException_whenBidderCannotBid() {
        startAuction();

        Bidder unauthorizedBidder = new Bidder(
                "unauthorized",
                "passwordHash",
                "unauthorized@example.com"
        ) {
            @Override
            public boolean canBid() {
                return false;
            }
        };

        assertThrows(
                InvalidBidException.class,
                () -> auction.registerAutoBid(
                        unauthorizedBidder,
                        new BigDecimal("12000000")
                )
        );
    }
}
