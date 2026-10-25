package com.auction.service;

import com.auction.model.entity.Auction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static AuctionManager instance = null;
    private final Map<UUID, Auction> auctions = new ConcurrentHashMap<>();
    // ============== SINGLETON ==============
    private AuctionManager(){}
    public synchronized static AuctionManager getInstance(){
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    public void add(Auction auction) {
        auctions.put(auction.getId(), auction);
    }

    public Optional<Auction> get(UUID id) {
        return Optional.ofNullable(auctions.get(id));
    }

    public List<Auction> findAll() {
        return new ArrayList<>(auctions.values());
    }
}
