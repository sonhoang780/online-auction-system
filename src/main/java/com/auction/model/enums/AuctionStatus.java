package com.auction.model.enums;

/**
 * Vòng đời phiên đấu giá: OPEN -> RUNNING -> FINISHED -> PAID.
 * CANCELED là trạng thái kết thúc, vào được từ OPEN, RUNNING hoặc FINISHED (chưa trả tiền).
 */
public enum AuctionStatus {
    OPEN, RUNNING, FINISHED, PAID, CANCELED;

    /**
     * Chặn chuyển trạng thái sai, ví dụ PAID quay lại RUNNING.
     */
    public boolean canTransitionTo(AuctionStatus next) {
        //Trong Enum, this đại diện cho trạng thái hiện tại của chính đối tượng đang gọi phương thức.
        //Ví dụ: Nếu bạn viết status.canTransitionTo(...) mà biến status đang là OPEN, thì lúc này this chính là OPEN.
        return switch (this) {
            case OPEN -> next == RUNNING || next == CANCELED;
            case RUNNING -> next == FINISHED || next == CANCELED;
            case FINISHED -> next == PAID || next == CANCELED;
            case PAID, CANCELED -> false;
        };
    }
}
