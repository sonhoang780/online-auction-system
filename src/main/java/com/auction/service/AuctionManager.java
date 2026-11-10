package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.model.entity.Auction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager implements AuctionDao {
    private static AuctionManager instance = null;
    private final Map<UUID, Auction> auctions = new ConcurrentHashMap<>();
    // ============== SINGLETON ==============
    private AuctionManager(){}
    public synchronized static AuctionManager getInstance(){
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    @Override
    public void save(Auction auction) {
        auctions.put(auction.getId(), auction);
    }

    @Override
    public Optional<Auction> findById(UUID id) {
        return Optional.ofNullable(auctions.get(id));
    }

    @Override
    public List<Auction> findAll() {
        return new ArrayList<>(auctions.values());
    }
}
