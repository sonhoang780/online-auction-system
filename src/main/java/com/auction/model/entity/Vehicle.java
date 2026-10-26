package com.auction.model.entity;

import com.auction.model.enums.ItemCategory;
import com.auction.model.enums.ItemCondition;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class Vehicle extends Item{
    private static final long serialVersionUID = 1L;

    private final String make;
    private final String model;   // có thể null nếu không rõ
    private final Integer year;
    private final Integer mileageKm;

    public Vehicle(String name, String description, UUID sellerId,
               BigDecimal startingPrice, List<String> images,
               ItemCondition condition,
               String make, String model, Integer year, Integer mileageKm) {
        super(name, description, sellerId, startingPrice, images,
                ItemCategory.VEHICLE, condition);
        this.make = make;
        this.model = model;
        this.year = year;
        this.mileageKm = mileageKm;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMileageKm() {
        return mileageKm;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Hãng: %s | Dòng xe: %s | Năm: %s | Đã đi: %s km",
                make,
                model == null ? "?" : model,
                year == null ? "?" : year.toString(),
                mileageKm == null ? "?" : mileageKm.toString());
    }
}
