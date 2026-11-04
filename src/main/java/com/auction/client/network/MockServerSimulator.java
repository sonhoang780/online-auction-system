package com.auction.client.network;

import javafx.application.Platform;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Công cụ giả lập Server phát tin Realtime (dùng cho tuần 6-10 khi Server B đang code).
 * Giúp người làm phần C tự kiểm tra UI, LineChart và xử lý đa luồng.
 */
public class MockServerSimulator {
    private static Thread simulationThread;
    private static boolean isRunning = false;
    private static double currentMockPrice = 5000000;
    private static final Random random = new Random();

    public static synchronized void startRealtimeBidSimulation(String auctionId) {
        if (isRunning) return;
        isRunning = true;

        simulationThread = new Thread(() -> {
            int bidCounter = 1;
            while (isRunning) {
                try {
                    // Giả lập mỗi 3 đến 5 giây có một người đặt giá mới
                    Thread.sleep(3000 + random.nextInt(2000));
                    if (!isRunning) break;

                    currentMockPrice += 100000 + (random.nextInt(5) * 50000);
                    String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String bidder = "user_auto_" + (10 + random.nextInt(90));

                    // Tạo JSON format giống thật của phần B
                    String mockJson = String.format(
                            "{\"type\":\"BID_BROADCAST\",\"auctionId\":\"%s\",\"price\":%.0f,\"bidder\":\"%s\",\"time\":\"%s\",\"count\":%d}",
                            auctionId, currentMockPrice, bidder, time, bidCounter++
                    );

                    // Bắn về UI thông qua ClientSocketService
                    Platform.runLater(() -> ClientSocketService.getInstance().notifyListeners(mockJson));

                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    public static synchronized void stopSimulation() {
        isRunning = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
    }
}
