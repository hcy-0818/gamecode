package com.example.account.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class TestController {
    @GetMapping("/api/test-datetime")
    public Map<String, Object> test() {
        return Map.of("now", LocalDateTime.now());
    }
}
