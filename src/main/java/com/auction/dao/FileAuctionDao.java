package com.auction.dao;

import com.auction.model.entity.Auction;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lưu phiên đấu giá ra file bằng Java Serialization (xem SerializedFile).
 * <p>
 * Dữ liệu vẫn nằm trong {@code memory} (thường là AuctionManager) để AuctionScheduler thấy cùng các phiên;
 * lớp này chỉ thêm việc nạp file lúc khởi động và ghi file mỗi lần save.
 * Scheduler đổi trạng thái mà không gọi save cũng không sao: khởi động lại thì finishIfExpired chạy lại.
 */
public class FileAuctionDao implements AuctionDao {
    private final SerializedFile<Auction> file;
    private final AuctionDao memory;

    public FileAuctionDao(Path file, AuctionDao memory) {
        this.file = new SerializedFile<>(file);
        this.memory = Objects.requireNonNull(memory, "memory must not be null");
        this.file.readAll().forEach(memory::save);
    }

    @Override
    public synchronized void save(Auction auction) {
        memory.save(auction);
        file.writeAll(memory.findAll());
    }

    @Override
    public Optional<Auction> findById(UUID id) {
        return memory.findById(id);
    }

    @Override
    public List<Auction> findAll() {
        return memory.findAll();
    }
}
