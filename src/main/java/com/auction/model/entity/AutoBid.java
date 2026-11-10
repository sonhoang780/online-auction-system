package com.auction.model.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record AutoBid(
        Bidder bidder,
        BigDecimal maxAmount,
        LocalDateTime registeredAt
) implements Serializable {
    public AutoBid {
        Objects.requireNonNull(bidder, "bidder must not be null");
        Objects.requireNonNull(maxAmount, "maxAmount must not be null");
        Objects.requireNonNull(registeredAt, "registeredAt must not be null");

        if (maxAmount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "maxAmount phải lớn hơn 0"
            );
        }
    }
}
