package com.sentinel.backend.dto;

import com.sentinel.backend.util.JsonUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standard REST API response wrapper.
 */
public class ApiResponse {

    private final boolean success;
    private final String message;
    private final Object data;
    private final long timestamp;

    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static ApiResponse ok(String message, Object data) {
        return new ApiResponse(true, message, data);
    }

    public static ApiResponse ok(Object data) {
        return new ApiResponse(true, "Operation successful", data);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message, null);
    }

    public static ApiResponse error(String message, Object errorDetails) {
        return new ApiResponse(false, message, errorDetails);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", success);
        map.put("message", message);
        map.put("data", data);
        map.put("timestamp", timestamp);
        return JsonUtils.toJson(map);
    }
}
