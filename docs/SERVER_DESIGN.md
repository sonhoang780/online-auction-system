# Thiết kế Module B – Server & Mạng (`com.auction.server`)

Tài liệu này mô tả thiết kế lớp (UML Class Diagram) cho **Bước 1 (Giao thức JSON)** và **Bước 2 (Server Socket đa luồng)**.
*(GitHub hỗ trợ hiển thị trực tiếp biểu đồ Mermaid dưới đây).*

## Sơ đồ lớp (UML Class Diagram) — Bước 1 & Bước 2

```mermaid
classDiagram
    direction TB

    namespace com_auction_server_protocol {
        class ProtocolType {
            <<final>>
            +String LOGIN$ = "LOGIN"
            +String REGISTER$ = "REGISTER"
            +String LOGOUT$ = "LOGOUT"
            +String LIST_AUCTIONS$ = "LIST_AUCTIONS"
            +String CREATE_AUCTION$ = "CREATE_AUCTION"
            +String PLACE_BID$ = "PLACE_BID"
            +String RESPONSE$ = "RESPONSE"
            +String BID_BROADCAST$ = "BID_BROADCAST"
            +String STATUS_BROADCAST$ = "STATUS_BROADCAST"
            +String ERROR$ = "ERROR"
        }

        class SocketRequest {
            -String action
            -String username
            -String password
            -String email
            -String role
            -String auctionId
            -BigDecimal amount
            -String title
            -BigDecimal startPrice
            -BigDecimal stepPrice
            +getAction() String
            +getAuctionId() String
            +getAmount() BigDecimal
        }

        class SocketResponse {
            -String type
            -String action
            -Boolean success
            -String message
            -String auctionId
            -BigDecimal price
            -String bidder
            -String time
            -Integer count
            -Object data
            +ok(String action, String msg, Object data)$ SocketResponse
            +error(String action, String errorMsg)$ SocketResponse
            +bidBroadcast(String id, BigDecimal price, String bidder, String time, int count)$ SocketResponse
            +statusBroadcast(String id, String newStatus, String msg)$ SocketResponse
        }

        class JsonUtils {
            <<final>>
            -Gson GSON$
            +toJson(Object obj)$ String
            +fromJson(String json, Class~T~ clazz)$ T
        }
    }

    namespace com_auction_server {
        class AuctionServer {
            +int DEFAULT_PORT$ = 8888
            -int port
            -ExecutorService clientPool
            -List~ClientHandler~ activeClients
            -AuctionService auctionService
            -ServerSocket serverSocket
            -boolean running
            +start() void
            +broadcast(SocketResponse response) void
            +broadcastRawJson(String jsonLine) void
            +removeClient(ClientHandler client) void
            +stop() void
            +main(String[] args)$ void
        }

        class ClientHandler {
            <<Runnable>>
            -Socket socket
            -AuctionServer server
            -BufferedReader in
            -PrintWriter out
            -boolean connected
            -User currentUser
            +run() void
            -handleIncomingJson(String rawJson) void
            +sendResponse(SocketResponse response) void
            +sendRawJson(String jsonLine) void
            +closeConnection() void
        }
    }

    AuctionServer "1" *-- "0..*" ClientHandler : quản lý (activeClients)
    ClientHandler --> AuctionServer : gọi ngược (server)
    ClientHandler ..> SocketRequest : đọc từ Client
    ClientHandler ..> SocketResponse : trả về Client
    ClientHandler ..> ProtocolType : phân nhánh action
    ClientHandler ..> JsonUtils : parse / serialize
    AuctionServer ..> SocketResponse : broadcast
    AuctionServer ..> JsonUtils : serialize
    SocketResponse ..> ProtocolType : dùng hằng số type
```
