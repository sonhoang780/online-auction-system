package com.auction.server.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích chuyển đổi qua lại giữa Object Java và chuỗi JSON (Serialization / Deserialization).
 * Hỗ trợ sẵn kiểu LocalDateTime của Java 17+ và đảm bảo JSON xuất ra nằm trên 1 dòng duy nhất
 * để truyền qua TCP Socket (BufferedReader.readLine()).
 */
public final class JsonUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Gson là thread-safe nên có thể dùng chung 1 biến static cho toàn bộ các luồng ClientHandler
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            new JsonPrimitive(src.format(ISO_FORMATTER)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                            LocalDateTime.parse(json.getAsString(), ISO_FORMATTER))
            .create();

    private JsonUtils() {}

    /**
     * Chuyển một Object Java bất kỳ thành chuỗi JSON (trên 1 dòng, không xuống dòng).
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /**
     * Đọc chuỗi JSON và chuyển thành Object Java theo class chỉ định.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static Gson getGson() {
        return GSON;
    }
}
