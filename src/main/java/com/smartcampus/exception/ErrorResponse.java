package com.smartcampus.exception;

import java.util.HashMap;
import java.util.Map;

public class ErrorResponse {
    public static Map<String, Object> of(int status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }
}
