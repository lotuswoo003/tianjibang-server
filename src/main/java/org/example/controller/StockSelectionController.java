package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.SelectedStockPageRequest;
import org.example.dto.StockSelectionResult;
import org.example.entity.SelectedStock;
import org.example.enums.StockSelectionStrategy;
import org.example.service.SelectedStockService;
import org.example.service.StockSelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock-selection")
@Tag(name = "选股策略", description = "根据不同策略筛选股票")
public class StockSelectionController {

    @Autowired
    private StockSelectionService stockSelectionService;

    @Autowired
    private SelectedStockService selectedStockService;

    @Autowired
    private org.example.service.SelectionPerformanceService selectionPerformanceService;

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
    @Operation(summary = "根据策略代码选股", description = "基于指定日期收盘进行选股。如果已有缓存数据则直接返回，否则查询数据库已选股票表")
    @Parameter(name = "strategyCode", description = "策略代码，如: CONSECUTIVE_LIMIT_UP")
    @Parameter(name = "selectionDate", description = "选股日期（格式：yyyy-MM-dd），表示基于该日期收盘进行选股")
    public ResponseEntity<Map<String, Object>> selectStocks(
            @PathVariable String strategyCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date selectionDate) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 从数据库查询该策略和日期的选股结果
            List<SelectedStock> selectedStocks = selectedStockService.findByStrategyAndDate(strategyCode, selectionDate);

            if (selectedStocks.isEmpty()) {
                response.put("success", true);
                response.put("strategyCode", strategyCode);
                response.put("selectionDate", selectionDate);
                response.put("count", 0);
                response.put("results", Collections.emptyList());
                response.put("message", "暂无该策略在此日期的选股记录，请先调用同步接口");
                return ResponseEntity.ok(response);
            }

            // 转换为统一的返回格式
            List<Map<String, Object>> results = selectedStocks.stream().map(stock -> {
                Map<String, Object> item = new HashMap<>();
                item.put("stockCode", stock.getStockCode());
                item.put("stockName", stock.getStockName());
                item.put("selectionDate", stock.getSelectionDate());
                item.put("selectionClosePrice", stock.getSelectionClosePrice());
                item.put("nextDayOpenPrice", stock.getNextDayOpenPrice());
                item.put("remark", stock.getRemark());
                item.put("gain1day", stock.getGain1day());
                item.put("gain3day", stock.getGain3day());
                item.put("gain7day", stock.getGain7day());
                return item;
            }).collect(Collectors.toList());

            response.put("success", true);
            response.put("strategyCode", strategyCode);
            response.put("selectionDate", selectionDate);
            response.put("count", results.size());
            response.put("results", results);
            response.put("source", "database");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
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

    @PostMapping("/sync")
    @Operation(summary = "同步策略选股结果", description = "异步执行策略选股并将结果保存到已选股票表，立即返回处理中状态")
    public ResponseEntity<Map<String, Object>> syncStrategies(
            @RequestParam List<String> strategyCodes,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date selectionDate) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 异步执行同步任务
            selectedStockService.syncStrategiesAsync(strategyCodes, selectionDate);

            response.put("success", true);
            response.put("message", "已接收到请求，正在后台处理中，请稍后查看结果");
            response.put("totalStrategies", strategyCodes.size());
            response.put("selectionDate", selectionDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "请求处理失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询已选股票", description = "支持按策略代码和日期筛选的分页查询")
    public ResponseEntity<SelectedStockService.PageResult<SelectedStock>> getSelectedStocksPage(
            @RequestBody SelectedStockPageRequest request) {

        int page = request.getPage() != null ? request.getPage() : 1;
        int size = request.getSize() != null ? request.getSize() : 20;

        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 20;
        }

        SelectedStockService.PageResult<SelectedStock> result =
                selectedStockService.findByPage(request.getStrategyCode(), request.getSelectionDate(), page, size);

        return ResponseEntity.ok(result);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "更新已选股票信息", description = "更新已选股票的备注、走势等信息")
    public ResponseEntity<Map<String, Object>> updateSelectedStock(
            @PathVariable @Parameter(description = "已选股票ID") Long id,
            @RequestBody SelectedStock selectedStock) {
        Map<String, Object> response = new HashMap<>();

        try {
            SelectedStock existing = selectedStockService.findById(id);
            if (existing == null) {
                response.put("success", false);
                response.put("message", "记录不存在");
                return ResponseEntity.badRequest().body(response);
            }

            selectedStock.setId(id);
            boolean updated = selectedStockService.update(selectedStock);

            if (updated) {
                response.put("success", true);
                response.put("message", "更新成功");
                response.put("data", selectedStockService.findById(id));
            } else {
                response.put("success", false);
                response.put("message", "更新失败");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/calculate-performance")
    @Operation(summary = "计算选股涨跌幅",
               description = "计算指定策略和日期的选股表现，以次日开盘价为基准，计算1天、3天、7天后的涨跌幅")
    public ResponseEntity<Map<String, Object>> calculatePerformance(
            @RequestParam @Parameter(description = "策略代码") String strategyCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "选股日期（格式：yyyy-MM-dd）") Date selectionDate) {
        Map<String, Object> response = new HashMap<>();

        try {
            int updatedCount = selectionPerformanceService.calculateAndUpdatePerformanceByStrategyAndDate(
                    strategyCode, selectionDate);

            response.put("success", true);
            response.put("message", "涨跌幅计算完成");
            response.put("strategyCode", strategyCode);
            response.put("selectionDate", selectionDate);
            response.put("updatedCount", updatedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
