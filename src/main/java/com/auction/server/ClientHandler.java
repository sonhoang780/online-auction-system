package com.auction.server;

import com.auction.model.entity.User;
import com.auction.server.protocol.JsonUtils;
import com.auction.server.protocol.ProtocolType;
import com.auction.server.protocol.SocketRequest;
import com.auction.server.protocol.SocketResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Đại diện cho một kết nối của 1 Client trên Server (chạy trên một luồng riêng trong ThreadPool).
 * Nhiệm vụ:
 * 1. Đọc từng dòng JSON (Request) từ Client gửi lên qua BufferedReader.readLine().
 * 2. Phân tích trường "action" và điều hướng xử lý.
 * 3. Gửi phản hồi (Response) hoặc bản tin Broadcast về lại cho Client qua PrintWriter.println().
 */
public class ClientHandler implements Runnable {
    private static final System.Logger LOG = System.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final AuctionServer server;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean connected = true;

    // Lưu thông tin người dùng sau khi đăng nhập thành công trên kết nối này
    private User currentUser;

    public ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        try {
            // Sử dụng UTF-8 để không bị lỗi font tiếng Việt khi truyền JSON qua mạng
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String rawJson;
            // Vòng lặp đọc từng gói tin JSON (mỗi gói tin nằm trên 1 dòng kết thúc bằng \n)
            while (connected && (rawJson = in.readLine()) != null) {
                rawJson = rawJson.trim();
                if (rawJson.isEmpty()) {
                    continue;
                }
                LOG.log(System.Logger.Level.INFO, "[Recv từ {0}]: {1}", clientAddress, rawJson);
                handleIncomingJson(rawJson);
            }
        } catch (IOException e) {
            if (connected) {
                LOG.log(System.Logger.Level.INFO, "Client {0} đã ngắt kết nối: {1}", clientAddress, e.getMessage());
            }
        } finally {
            closeConnection();
        }
    }

    /**
     * Phân tích gói tin JSON nhận được từ Client và điều hướng theo "action".
     */
    private void handleIncomingJson(String rawJson) {
        SocketRequest request;
        try {
            request = JsonUtils.fromJson(rawJson, SocketRequest.class);
        } catch (Exception e) {
            sendResponse(SocketResponse.error("PARSE_ERROR", "Gói tin JSON không hợp lệ: " + e.getMessage()));
            return;
        }

        if (request == null || request.getAction() == null || request.getAction().isBlank()) {
            sendResponse(SocketResponse.error("INVALID_REQUEST", "Thiếu trường 'action' trong gói tin JSON"));
            return;
        }

        String action = request.getAction().trim().toUpperCase();
        try {
            switch (action) {
                case ProtocolType.LOGIN -> handleLogin(request);
                case ProtocolType.REGISTER -> handleRegister(request);
                case ProtocolType.LOGOUT -> handleLogout();
                case ProtocolType.LIST_AUCTIONS -> handleListAuctions();
                case ProtocolType.GET_AUCTION -> handleGetAuction(request);
                case ProtocolType.CREATE_AUCTION -> handleCreateAuction(request);
                case ProtocolType.PLACE_BID -> handlePlaceBid(request);
                case ProtocolType.APPROVE_AUCTION -> handleApproveAuction(request);
                case ProtocolType.REJECT_AUCTION -> handleRejectAuction(request);
                default -> sendResponse(SocketResponse.error(action, "Hành động không được hỗ trợ: " + action));
            }
        } catch (Exception e) {
            LOG.log(System.Logger.Level.WARNING, "Lỗi khi xử lý action " + action, e);
            sendResponse(SocketResponse.error(action, e.getMessage()));
        }
    }

    // ============== CÁC HÀM XỬ LÝ ACTION (Sẽ ghép DAO & AuctionService ở Bước 3, 4) ==============

    private void handleLogin(SocketRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            sendResponse(SocketResponse.error(ProtocolType.LOGIN, "Vui lòng nhập tên đăng nhập"));
            return;
        }
        // Khung phản hồi Bước 2 (Bước 4 sẽ ghép AuthService & UserDao)
        sendResponse(SocketResponse.ok(ProtocolType.LOGIN,
                "Đăng nhập thành công với tài khoản: " + req.getUsername(), req.getUsername()));
    }

    private void handleRegister(SocketRequest req) {
        sendResponse(SocketResponse.ok(ProtocolType.REGISTER, "Đăng ký tài khoản thành công", req.getUsername()));
    }

    private void handleLogout() {
        this.currentUser = null;
        sendResponse(SocketResponse.ok(ProtocolType.LOGOUT, "Đã đăng xuất", null));
    }

    private void handleListAuctions() {
        sendResponse(SocketResponse.ok(ProtocolType.LIST_AUCTIONS,
                "Danh sách phiên đấu giá", server.getAuctionService().listAuctions()));
    }

    private void handleGetAuction(SocketRequest req) {
        sendResponse(SocketResponse.ok(ProtocolType.GET_AUCTION, "Thông tin phiên đấu giá", req.getAuctionId()));
    }

    private void handleCreateAuction(SocketRequest req) {
        sendResponse(SocketResponse.ok(ProtocolType.CREATE_AUCTION, "Đã nhận yêu cầu tạo phiên đấu giá", req.getTitle()));
    }

    private void handlePlaceBid(SocketRequest req) {
        sendResponse(SocketResponse.ok(ProtocolType.PLACE_BID, "Đã nhận yêu cầu đặt giá", req.getAmount()));
    }

    private void handleApproveAuction(SocketRequest req) {
        sendResponse(SocketResponse.ok(ProtocolType.APPROVE_AUCTION, "Đã duyệt phiên đấu giá", req.getAuctionId()));
    }

    private void handleRejectAuction(SocketRequest req) {
        sendResponse(SocketResponse.ok(ProtocolType.REJECT_AUCTION, "Đã từ chối phiên đấu giá", req.getAuctionId()));
    }

    // ============== GỬI DỮ LIỆU VỀ CLIENT ==============

    /**
     * Gửi một đối tượng SocketResponse dưới dạng chuỗi JSON 1 dòng về cho Client này.
     * Đồng bộ hóa (synchronized) trên từng ClientHandler để tránh 2 luồng (luồng phản hồi
     * trực tiếp và luồng Broadcast) cùng ghi đè lên 1 Socket cùng lúc.
     */
    public void sendResponse(SocketResponse response) {
        sendRawJson(JsonUtils.toJson(response));
    }

    public synchronized void sendRawJson(String jsonLine) {
        if (connected && out != null) {
            out.println(jsonLine);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isConnected() {
        return connected && !socket.isClosed();
    }

    /**
     * Đóng kết nối an toàn và tự xóa khỏi danh sách activeClients của Server.
     */
    public void closeConnection() {
        if (!connected) {
            return;
        }
        connected = false;
        server.removeClient(this);
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        if (out != null) {
            out.close();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }
}
