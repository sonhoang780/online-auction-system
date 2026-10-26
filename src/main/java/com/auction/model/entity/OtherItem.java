package com.auction.model.entity;

import com.auction.model.enums.ItemCategory;
import com.auction.model.enums.ItemCondition;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Item cho FASHION / COLLECTIBLE / OTHER - chưa có thuộc tính riêng nên chỉ giữ extraInfo dạng text tự do.
 * Category nào cần thuộc tính có cấu trúc thì tách subclass riêng (xem ItemFactory).
 */
public class OtherItem extends Item {

    private static final long serialVersionUID = 1L;

    private final String extraInfo;   // có thể null

    public OtherItem(String name, String description, UUID sellerId,
                     BigDecimal startingPrice, List<String> images,
                     ItemCategory category, ItemCondition condition,
                     String extraInfo) {
        super(name, description, sellerId, startingPrice, images, category, condition);
        if (category == ItemCategory.ELECTRONICS || category == ItemCategory.ART
                || category == ItemCategory.VEHICLE) {
            throw new IllegalArgumentException(category + " đã có lớp riêng, không dùng OtherItem");
        }
        this.extraInfo = extraInfo;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    @Override
    public String getSpecificInfo() {
        return "Thông tin thêm: " + (extraInfo == null ? "(không có)" : extraInfo);
    }
}
