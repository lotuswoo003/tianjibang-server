package org.example.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        resp.put("timestamp", Instant.now().toString());
        return resp;
    }

    @GetMapping("/api/hello")
    public Map<String, Object> hello() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Hello from Spring Boot!");
        return resp;
    }
}

