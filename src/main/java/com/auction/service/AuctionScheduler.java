package com.auction.service;

import com.auction.model.entity.Auction;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {
    private boolean started = false;
    private boolean stopped = false;

    private final AuctionManager auctionManager =
            AuctionManager.getInstance();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

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
                System.err.println(
                        "Lỗi khi kiểm tra phiên đấu giá "
                        + auction.getId() + ": " + e.getMessage()
                );
                e.printStackTrace();
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