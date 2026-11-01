package com.auction.client.model;

/**
 * Model biểu diễn một phiên / sản phẩm đấu giá.
 */
public class AuctionItem {
    private String id;
    private String title;
    private String description;
    private double currentPrice;
    private double stepPrice;
    private String highestBidder;
    private String status; // "UPCOMING", "ACTIVE", "ENDED"
    private int remainingSeconds;

    public AuctionItem(String id, String title, String description, double currentPrice, double stepPrice, String highestBidder, String status, int remainingSeconds) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.currentPrice = currentPrice;
        this.stepPrice = stepPrice;
        this.highestBidder = highestBidder;
        this.status = status;
        this.remainingSeconds = remainingSeconds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getStepPrice() {
        return stepPrice;
    }

    public void setStepPrice(double stepPrice) {
        this.stepPrice = stepPrice;
    }

    public String getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(String highestBidder) {
        this.highestBidder = highestBidder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }
}
