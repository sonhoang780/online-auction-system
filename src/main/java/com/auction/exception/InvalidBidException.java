package com.auction.exception;

// ponytail: bản tối thiểu để Auction compile, D thay bằng bản chính thức tuần 7
public class InvalidBidException extends RuntimeException {
    public InvalidBidException(String message) {
        super(message);
    }
}
