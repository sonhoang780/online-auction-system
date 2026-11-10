package com.auction.dao;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Đọc/ghi cả một danh sách object ra một file bằng Java Serialization. Dùng chung cho FileAuctionDao và FileUserDao.
 * <p>
 * Đọc lại không cần constructor "restore": ObjectInputStream không gọi constructor của lớp Serializable,
 * mà ghi thẳng giá trị từng field (kể cả final) từ file vào, nên id, createdAt... giữ nguyên.
 */
final class SerializedFile<T extends Serializable> {
    private final Path file;

    SerializedFile(Path file) {
        this.file = Objects.requireNonNull(file, "file must not be null");
    }

    /**
     * Chưa có file thì trả về danh sách rỗng.
     * File hỏng thì ném lỗi, KHÔNG trả về rỗng: nếu chạy tiếp với danh sách rỗng, lần ghi sau sẽ đè mất dữ liệu cũ.
     */
    @SuppressWarnings("unchecked")
    List<T> readAll() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (List<T>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Không đọc được file dữ liệu " + file, e);
        }
    }

    // ponytail: ghi lại cả file mỗi lần, đủ cho vài nghìn object; nhiều hơn thì chuyển sang DB.
    // Ghi ra file tạm rồi move đè: server chết giữa lúc ghi thì file cũ vẫn còn nguyên.
    void writeAll(Collection<? extends T> items) {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(new ArrayList<>(items));
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Không ghi được file dữ liệu " + file, e);
        }
    }
}
