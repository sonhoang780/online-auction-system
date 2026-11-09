package com.auction.server;

import com.auction.server.protocol.JsonUtils;
import com.auction.server.protocol.SocketResponse;
import com.auction.service.AuctionService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Máy chủ TCP Socket đa luồng (Multi-threaded TCP Server) cho Hệ thống Đấu giá Trực tuyến.
 * - Lắng nghe kết nối từ nhiều Client (phần C) trên cổng mặc định 8888.
 * - Sử dụng ThreadPool (ExecutorService) để xử lý đồng thời nhiều Client.
 * - Quản lý danh sách Client đang online (CopyOnWriteArrayList) để phát Broadcast Realtime.
 */
public class AuctionServer {
    private static final System.Logger LOG = System.getLogger(AuctionServer.class.getName());
    public static final int DEFAULT_PORT = 8888;

    private final int port;
    private final ExecutorService clientPool;
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    private final AuctionService auctionService = new AuctionService();

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public AuctionServer(int port) {
        this.port = port;
        // CachedThreadPool tự động tạo luồng mới khi có Client mới và tái sử dụng luồng rảnh
        this.clientPool = Executors.newCachedThreadPool();
    }

    /**
     * Khởi chạy Server lắng nghe kết nối từ các Client.
     */
    public void start() {
        running = true;
        try {
            serverSocket = new ServerSocket(port);
            LOG.log(System.Logger.Level.INFO,
                    "=== AuctionServer đã khởi động tại cổng {0}, sẵn sàng nhận kết nối ===", port);

            while (running) {
                try {
                    // Chờ (block) cho đến khi có 1 Client mới kết nối vào
                    Socket clientSocket = serverSocket.accept();
                    LOG.log(System.Logger.Level.INFO,
                            "Client mới kết nối từ: {0} (Tổng online: {1})",
                            clientSocket.getRemoteSocketAddress(), activeClients.size() + 1);

                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    activeClients.add(handler);
                    clientPool.execute(handler);
                } catch (IOException e) {
                    if (running) {
                        LOG.log(System.Logger.Level.WARNING, "Lỗi khi chấp nhận kết nối Client", e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, "Không thể mở cổng Server " + port, e);
        } finally {
            stop();
        }
    }

    /**
     * Phát (Broadcast) một gói tin SocketResponse tới TẤT CẢ các Client đang kết nối.
     */
    public void broadcast(SocketResponse response) {
        String jsonLine = JsonUtils.toJson(response);
        broadcastRawJson(jsonLine);
    }

    /**
     * Phát chuỗi JSON tới tất cả Client đang kết nối.
     */
    public void broadcastRawJson(String jsonLine) {
        for (ClientHandler client : activeClients) {
            if (client.isConnected()) {
                client.sendRawJson(jsonLine);
            }
        }
    }

    /**
     * Xóa Client khỏi danh sách active khi Client ngắt kết nối.
     */
    public void removeClient(ClientHandler client) {
        activeClients.remove(client);
        LOG.log(System.Logger.Level.INFO, "Đã xóa 1 Client. Số Client đang online: {0}", activeClients.size());
    }

    public int getActiveClientCount() {
        return activeClients.size();
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    /**
     * Dừng Server an toàn: đóng tất cả kết nối Client, đóng ServerSocket và tắt ThreadPool.
     */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        LOG.log(System.Logger.Level.INFO, "Đang dừng AuctionServer...");

        for (ClientHandler client : activeClients) {
            client.closeConnection();
        }
        activeClients.clear();

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        clientPool.shutdownNow();
        LOG.log(System.Logger.Level.INFO, "AuctionServer đã dừng hoàn toàn.");
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        AuctionServer server = new AuctionServer(port);
        // Đăng ký Shutdown Hook để khi bấm Ctrl+C tắt Server thì các kết nối được đóng sạch sẽ
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "ServerShutdownHook"));
        server.start();
    }
}
