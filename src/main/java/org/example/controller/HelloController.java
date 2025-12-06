package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "系统接口", description = "健康检查和基础功能")
public class HelloController {

    @GetMapping("/api/health")
    @Operation(summary = "健康检查", description = "检查服务运行状态")
    public Map<String, Object> health() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        resp.put("timestamp", Instant.now().toString());
        return resp;
    }

    @GetMapping("/api/hello")
    @Operation(summary = "Hello接口", description = "测试接口连通性")
    public Map<String, Object> hello() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Hello from Spring Boot!");
        return resp;
    }
}

