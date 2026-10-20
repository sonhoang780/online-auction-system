package com.auction.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp gốc cho mọi đối tượng có định danh trong hệ thống (User, Item, Auction, ...).
 * Hai entity bằng nhau khi cùng loại và cùng id.
 */
public abstract class Entity {
    private final UUID id;
    private final LocalDateTime createdAt;

    protected Entity() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id.equals(((Entity) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
