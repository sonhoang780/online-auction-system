package com.auction.dao;

import com.auction.model.entity.Bidder;
import com.auction.model.entity.Seller;
import com.auction.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUserDaoTest {

    @TempDir
    Path dir;

    @Test
    void saveThenReload_keepsSubclassIdAndBalance() {
        Path file = dir.resolve("data/users.dat");
        Bidder bidder = new Bidder("an", "hash", "an@example.com");
        bidder.deposit(new BigDecimal("5000"));
        Seller seller = new Seller("binh", "hash", "binh@example.com");

        FileUserDao dao = new FileUserDao(file);
        dao.save(bidder);
        dao.save(seller);

        // Server khởi động lại: chỉ còn file.
        FileUserDao reloaded = new FileUserDao(file);
        User loaded = reloaded.findById(bidder.getId()).orElseThrow();

        assertInstanceOf(Bidder.class, loaded);
        assertEquals(0, new BigDecimal("5000").compareTo(((Bidder) loaded).getBalance()));
        assertEquals(seller.getId(), reloaded.findByUsername("binh").orElseThrow().getId());
        assertEquals(2, reloaded.findAll().size());
    }

    @Test
    void saveAgain_overwritesSameUser() {
        Path file = dir.resolve("users.dat");
        Bidder bidder = new Bidder("an", "hash", "an@example.com");
        FileUserDao dao = new FileUserDao(file);
        dao.save(bidder);

        bidder.deposit(new BigDecimal("100"));
        dao.save(bidder);

        FileUserDao reloaded = new FileUserDao(file);
        assertEquals(1, reloaded.findAll().size());
        assertEquals(0, new BigDecimal("100").compareTo(((Bidder) reloaded.findAll().get(0)).getBalance()));
        assertTrue(reloaded.findByUsername("khong-co").isEmpty());
    }
}
