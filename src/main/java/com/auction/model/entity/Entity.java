package com.auction.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Lớp gốc cho mọi đối tượng có định danh trong hệ thống (User, Item, Auction, ...).
 * Hai entity bằng nhau khi cùng loại và cùng id.
 */
public abstract class Entity {
    private final UUID id;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Entity() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    /**
     * Gọi method này MỖI KHI state của entity thay đổi.
     * Đặt protected để chỉ subclass và package gọi được, ngăn lạm dụng từ ngoài.
     */
    protected void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Restore entity từ DB - bắt buộc non-null để tránh data corrupt
     */
    protected Entity(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
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
