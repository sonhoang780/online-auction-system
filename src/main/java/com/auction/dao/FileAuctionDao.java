package com.auction.dao;

import com.auction.model.entity.Auction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lưu phiên đấu giá ra file bằng Java Serialization.
 * <p>
 * Đọc lại không cần constructor "restore": ObjectInputStream không gọi constructor của lớp Serializable,
 * mà ghi thẳng giá trị từng field (kể cả final) từ file vào, nên id, createdAt, giá, bids giữ nguyên.
 * <p>
 * Dữ liệu vẫn nằm trong {@code memory} (thường là AuctionManager) để AuctionScheduler thấy cùng các phiên;
 * lớp này chỉ thêm việc nạp file lúc khởi động và ghi file mỗi lần save.
 * Scheduler đổi trạng thái mà không gọi save cũng không sao: khởi động lại thì finishIfExpired chạy lại.
 * <p>
 * Dùng ở server: {@code new AuctionService(new FileAuctionDao(Path.of("data/auctions.dat"), AuctionManager.getInstance()))}
 */
public class FileAuctionDao implements AuctionDao {
    private final Path file;
    private final AuctionDao memory;

    public FileAuctionDao(Path file, AuctionDao memory) {
        this.file = Objects.requireNonNull(file, "file must not be null");
        this.memory = Objects.requireNonNull(memory, "memory must not be null");
        load();
    }

    @Override
    public synchronized void save(Auction auction) {
        memory.save(auction);
        writeAll();
    }

    @Override
    public Optional<Auction> findById(UUID id) {
        return memory.findById(id);
    }

    @Override
    public List<Auction> findAll() {
        return memory.findAll();
    }

    // File hỏng thì ném lỗi, KHÔNG bỏ qua: nếu chạy tiếp với danh sách rỗng, lần save sau sẽ ghi đè mất dữ liệu cũ.
    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            ((List<Auction>) in.readObject()).forEach(memory::save);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Không đọc được file dữ liệu " + file, e);
        }
    }

    // ponytail: ghi lại cả file mỗi lần save, đủ cho vài nghìn phiên; nhiều hơn thì chuyển sang DB.
    // Ghi ra file tạm rồi move đè: server chết giữa lúc ghi thì file cũ vẫn còn nguyên.
    private void writeAll() {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(new ArrayList<>(memory.findAll()));
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Không ghi được file dữ liệu " + file, e);
        }
    }
}
