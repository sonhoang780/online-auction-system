package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.dao.DataFiles;
import com.auction.dao.UserDao;
import com.auction.model.entity.Auction;
import com.auction.model.entity.BidTransaction;
import com.auction.model.entity.Bidder;
import com.auction.model.entity.Item;
import com.auction.model.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Cửa vào nghiệp vụ đấu giá cho server (B): nhận id, tìm phiên qua AuctionDao rồi gọi xuống Auction.
 *
 * Không tự lock: lock và kiểm tra giá nằm trong Auction.placeBid, vì:
 * - Lock phải nằm cạnh dữ liệu nó bảo vệ. currentPrice, status, bids là của Auction; nếu lock đặt ở đây
 *   thì nơi nào gọi thẳng auction.placeBid() (AuctionScheduler, test, service khác) sẽ lọt qua lock.
 * - Mỗi phiên một lock riêng: đặt giá ở hai phiên khác nhau chạy song song được. Một lock chung
 *   ở service sẽ bắt mọi phiên xếp hàng chung.
 * - Kiểm tra "giá >= giá hiện tại + bước giá" và ghi giá mới phải trong cùng một lock; finish() của
 *   Scheduler cũng dùng lock đó, nên một bid không thể chen vào đúng lúc phiên đang đóng.
 * - Thêm một lock nữa ở đây là thừa, và lock lồng nhau dễ gây deadlock.
 */
public class AuctionService {
    private final AuctionDao dao;
    private final UserDao userDao;

    /**
     * Mặc định lưu ra file trong data/ (xem DataFiles).
     * Giữ constructor rỗng để server của B (new AuctionService()) không phải sửa.
     */
    public AuctionService() {
        this(DataFiles.auctions(), DataFiles.users());
    }

    /** Test truyền DAO riêng vào đây để không đụng tới data/ thật. */
    public AuctionService(AuctionDao dao, UserDao userDao) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
    }

    public Auction createAuction(Item item, LocalDateTime start, LocalDateTime end, BigDecimal increment) {
        Auction auction = new Auction(item, start, end, increment);
        dao.save(auction);
        return auction;
    }

    /**
     * Ném AuctionClosedException / InvalidBidException từ Auction, B bắt và trả lỗi về client.
     * Bid xong thì save lại để bản lưu file có giá mới.
     */
    public BidTransaction placeBid(UUID auctionId, Bidder bidder, BigDecimal amount) {
        Auction auction = getAuction(auctionId);
        BidTransaction bid = auction.placeBid(bidder, amount);
        dao.save(auction);
        return bid;
    }

    /**
     * Người thắng trả tiền cho phiên đã FINISHED. Server truyền id của user đang đăng nhập.
     * Lấy Bidder từ UserDao (bản dùng chung, không phải bản client gửi lên) rồi để Auction.markPaid kiểm tra
     * người thắng, trừ tiền và đổi trạng thái trong lock của phiên.
     */
    public void payAuction(UUID auctionId, UUID payerId) {
        Auction auction = getAuction(auctionId);
        User user = userDao.findById(payerId)
                .orElseThrow(() -> new IllegalArgumentException("Không có người dùng " + payerId));
        if (!(user instanceof Bidder bidder)) {
            throw new IllegalArgumentException("Chỉ người mua (Bidder) mới thanh toán được");
        }
        auction.markPaid(bidder);
        // ponytail: hai file không ghi cùng lúc được như transaction của DB. Ghi phiên (PAID) trước:
        // server chết giữa hai lần ghi thì người mua chưa bị trừ tiền trong file, còn phiên đã PAID nên
        // không trả lại lần nữa được. Ngược lại thì người mua có thể bị trừ tiền hai lần. Muốn chặt hơn thì dùng DB.
        dao.save(auction);
        userDao.save(bidder);
    }

    public Auction getAuction(UUID auctionId) {
        return dao.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không có phiên " + auctionId));
    }

    public List<Auction> listAuctions() {
        return dao.findAll();
    }
}
