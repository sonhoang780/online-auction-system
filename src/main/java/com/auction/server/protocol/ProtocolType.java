package com.auction.server.protocol;

/**
 * Các loại hành động (Action) mà Client gửi lên Server,
 * và các loại thông báo (MessageType) mà Server trả về / Broadcast.
 */
public final class ProtocolType {

    private ProtocolType() {}

    // ============== CLIENT -> SERVER (REQUEST ACTIONS) ==============
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String LOGOUT = "LOGOUT";
    public static final String LIST_AUCTIONS = "LIST_AUCTIONS";
    public static final String GET_AUCTION = "GET_AUCTION";
    public static final String CREATE_AUCTION = "CREATE_AUCTION";
    public static final String PLACE_BID = "PLACE_BID";
    public static final String APPROVE_AUCTION = "APPROVE_AUCTION";
    public static final String REJECT_AUCTION = "REJECT_AUCTION";

    // ============== SERVER -> CLIENT (MESSAGE TYPES) ==============
    /** Phản hồi trực tiếp cho 1 yêu cầu của Client */
    public static final String RESPONSE = "RESPONSE";
    /** Phát thông báo có người đặt giá mới cho toàn bộ Client đang xem (khớp với Client C) */
    public static final String BID_BROADCAST = "BID_BROADCAST";
    /** Phát thông báo khi phiên đấu giá đổi trạng thái (OPEN -> RUNNING -> CLOSED...) */
    public static final String STATUS_BROADCAST = "STATUS_BROADCAST";
    /** Thông báo lỗi từ Server */
    public static final String ERROR = "ERROR";
}
