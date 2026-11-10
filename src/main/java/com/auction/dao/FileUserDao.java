package com.auction.dao;

import com.auction.model.entity.User;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lưu user ra file bằng Java Serialization. Giữ một bản trong RAM để đọc nhanh, mỗi lần save ghi lại cả file.
 * Cả server chỉ nên có MỘT object cho mỗi file (lấy qua DataFiles.users()):
 * hai object cùng ghi một file sẽ đè mất dữ liệu của nhau.
 */
public class FileUserDao implements UserDao {
    private final SerializedFile<User> file;
    private final Map<UUID, User> users = new ConcurrentHashMap<>();

    public FileUserDao(Path file) {
        this.file = new SerializedFile<>(file);
        this.file.readAll().forEach(u -> users.put(u.getId(), u));
    }

    @Override
    public synchronized void save(User user) {
        users.put(user.getId(), user);
        file.writeAll(users.values());
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    // ponytail: duyệt cả map, đủ cho vài nghìn user; nhiều hơn thì thêm Map<String, User> theo username.
    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
}
