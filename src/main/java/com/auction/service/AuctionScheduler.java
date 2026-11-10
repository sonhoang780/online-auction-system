package com.auction.service;

import com.auction.model.entity.Auction;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {
    private static final System.Logger LOG = System.getLogger(AuctionScheduler.class.getName());

    private boolean started = false;
    private boolean stopped = false;

    private final AuctionManager auctionManager =
            AuctionManager.getInstance();

    // Daemon: không giữ JVM sống khi server tắt mà quên gọi stop().
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auction-scheduler");
                t.setDaemon(true);
                return t;
            });

    public synchronized void start() {
        if (stopped) {
            throw new IllegalStateException(
                    "Scheduler đã dừng và không thể khởi động lại"
            );
        }

        if (started) {
            return;
        }

        scheduler.scheduleAtFixedRate(
                this::checkAuctions,
                0,
                1,
                TimeUnit.SECONDS
        );

        started = true;
    }

    private void checkAuctions() {
        for (Auction auction : auctionManager.findAll()) {
            try {
                auction.startIfDue();
                auction.finishIfExpired();
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING,
                        "Lỗi khi kiểm tra phiên đấu giá " + auction.getId(), e);
            }
        }
    }

    public synchronized void stop() {
        if (stopped) {
            return;
        }

        stopped = true;
        scheduler.shutdown();
    }
}