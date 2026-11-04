package com.auction.client.network;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton quản lý kết nối TCP Socket tới Server (phần B).
 * Có Thread nền lắng nghe gói tin JSON từ Server và cập nhật UI qua Platform.runLater().
 */
public class ClientSocketService {
    private static ClientSocketService instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isRunning = false;
    private boolean isSimulationMode = false;

    // Danh sách callback an toàn khi chạy đa luồng (Thread-safe)
    private final List<ServerMessageListener> listeners = new CopyOnWriteArrayList<>();

    public interface ServerMessageListener {
        void onMessageReceived(String rawJson);
    }

    private ClientSocketService() {}

    public static synchronized ClientSocketService getInstance() {
        if (instance == null) {
            instance = new ClientSocketService();
        }
        return instance;
    }

    /**
     * Bật chế độ giả lập (Mock Server) để test UI khi Server B chưa sẵn sàng.
     */
    public void enableSimulationMode(boolean enable) {
        this.isSimulationMode = enable;
    }

    public boolean isSimulationMode() {
        return isSimulationMode;
    }

    /**
     * Kết nối tới Server B qua IP và Cổng.
     */
    public void connect(String host, int port) throws Exception {
        if (isSimulationMode) {
            System.out.println("[ClientSocketService] Đang ở chế độ Mock/Giả lập.");
            return;
        }

        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        isRunning = true;

        // Thread chạy ngầm (Daemon) để đọc dữ liệu từ Server mà không làm đơ giao diện
        Thread listenerThread = new Thread(() -> {
            try {
                String line;
                while (isRunning && (line = in.readLine()) != null) {
                    final String message = line;
                    // BẮT BUỘC: Đẩy về FX Application Thread để cập nhật UI an toàn
                    Platform.runLater(() -> notifyListeners(message));
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("[ClientSocketService] Mất kết nối tới Server: " + e.getMessage());
                }
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.setName("ClientSocketListenerThread");
        listenerThread.start();
    }

    /**
     * Gửi gói tin JSON lên Server.
     */
    public void sendMessage(String json) {
        if (isSimulationMode) {
            System.out.println("[Mock Gửi lên Server]: " + json);
            return;
        }

        if (out != null) {
            out.println(json);
        } else {
            System.err.println("[ClientSocketService] Chưa kết nối tới Server!");
        }
    }

    /**
     * Đăng ký Controller vào danh sách nhận sự kiện từ Server.
     */
    public void addListener(ServerMessageListener listener) {
        listeners.add(listener);
    }

    /**
     * Hủy đăng ký khi Controller bị đóng.
     */
    public void removeListener(ServerMessageListener listener) {
        listeners.remove(listener);
    }

    /**
     * Thông báo cho toàn bộ Controller đã đăng ký (chạy trên UI Thread).
     */
    public void notifyListeners(String json) {
        for (ServerMessageListener listener : listeners) {
            try {
                listener.onMessageReceived(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        isRunning = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }
}
