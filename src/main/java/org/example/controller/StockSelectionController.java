package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.StockSelectionResult;
import org.example.enums.StockSelectionStrategy;
import org.example.service.StockSelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock-selection")
@Tag(name = "选股策略", description = "根据不同策略筛选股票")
public class StockSelectionController {

    @Autowired
    private StockSelectionService stockSelectionService;

    @GetMapping("/strategies")
    @Operation(summary = "获取所有可用的选股策略", description = "返回所有可用的选股策略列表")
    public ResponseEntity<List<Map<String, String>>> getAllStrategies() {
        List<Map<String, String>> strategies = Arrays.stream(StockSelectionStrategy.values())
                .map(strategy -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("code", strategy.getCode());
                    map.put("description", strategy.getDescription());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(strategies);
    }

    @GetMapping("/select/{strategyCode}")
    @Operation(summary = "根据策略代码选股", description = "传入策略代码，返回符合条件的股票列表")
    @Parameter(name = "strategyCode", description = "策略代码，如: CONSECUTIVE_LIMIT_UP")
    public ResponseEntity<Map<String, Object>> selectStocks(@PathVariable String strategyCode) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<StockSelectionResult> results = stockSelectionService.selectStocks(strategyCode);
            response.put("success", true);
            response.put("strategyCode", strategyCode);
            response.put("count", results.size());
            response.put("results", results);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (UnsupportedOperationException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "选股失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/consecutive-limit-up")
    @Operation(summary = "连续涨停策略", description = "查询最近半年出现连续涨停的股票")
    public ResponseEntity<Map<String, Object>> findConsecutiveLimitUpStocks() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<StockSelectionResult> results = stockSelectionService.findConsecutiveLimitUpStocks();
            response.put("success", true);
            response.put("strategy", "连续涨停策略");
            response.put("count", results.size());
            response.put("results", results);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "选股失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/history-two-waves")
    @Operation(summary = "历史二波策略", description = "查询过去一年出现过连续涨停，之后上涨到高点回落调整后再次出现连续涨停的股票")
    public ResponseEntity<Map<String, Object>> findHistoryTwoWavesStocks() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<StockSelectionResult> results = stockSelectionService.findHistoryTwoWavesStocks();
            response.put("success", true);
            response.put("strategy", "历史二波策略");
            response.put("count", results.size());
            response.put("results", results);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "选股失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
