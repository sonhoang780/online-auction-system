package com.auction.server.protocol;

import java.math.BigDecimal;

/**
 * Gói tin JSON từ Server gửi về Client (Response hoặc Broadcast).
 * Thiết kế tương thích 100% với định dạng JSON mà Client (phần C) đang lắng nghe:
 * - Broadcast đặt giá:
 *   {"type":"BID_BROADCAST","auctionId":"AUC-001","price":5200000,"bidder":"user_a","time":"14:20:05","count":1}
 * - Phản hồi chung:
 *   {"type":"RESPONSE","action":"LOGIN","success":true,"message":"Đăng nhập thành công","data":...}
 */
public class SocketResponse {
    private String type;       // RESPONSE, BID_BROADCAST, STATUS_BROADCAST, ERROR
    private String action;     // LOGIN, REGISTER, PLACE_BID, LIST_AUCTIONS...
    private Boolean success;
    private String message;

    // Các trường phục vụ BID_BROADCAST & STATUS_BROADCAST (khớp với Client C)
    private String auctionId;
    private BigDecimal price;
    private String bidder;
    private String time;
    private Integer count;
    private String status;

    // Dữ liệu mở rộng (danh sách phiên, thông tin user...)
    private Object data;

    public SocketResponse() {}

    // ============== FACTORY METHODS TIỆN DỤNG ==============

    public static SocketResponse ok(String action, String message, Object data) {
        SocketResponse res = new SocketResponse();
        res.type = ProtocolType.RESPONSE;
        res.action = action;
        res.success = true;
        res.message = message;
        res.data = data;
        return res;
    }

    public static SocketResponse error(String action, String errorMessage) {
        SocketResponse res = new SocketResponse();
        res.type = ProtocolType.ERROR;
        res.action = action;
        res.success = false;
        res.message = errorMessage;
        return res;
    }

    public static SocketResponse bidBroadcast(String auctionId, BigDecimal price, String bidder, String time, int count) {
        SocketResponse res = new SocketResponse();
        res.type = ProtocolType.BID_BROADCAST;
        res.auctionId = auctionId;
        res.price = price;
        res.bidder = bidder;
        res.time = time;
        res.count = count;
        return res;
    }

    public static SocketResponse statusBroadcast(String auctionId, String newStatus, String message) {
        SocketResponse res = new SocketResponse();
        res.type = ProtocolType.STATUS_BROADCAST;
        res.auctionId = auctionId;
        res.status = newStatus;
        res.message = message;
        return res;
    }

    // ============== GETTERS & SETTERS ==============

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getBidder() {
        return bidder;
    }

    public void setBidder(String bidder) {
        this.bidder = bidder;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
