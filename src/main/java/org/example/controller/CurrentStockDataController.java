package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.entity.CurrentStockData;
import org.example.service.CurrentStockDataService;
import org.example.service.TechnicalIndicatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/current-stock")
@Tag(name = "当前股票数据管理", description = "管理最近1年的股票交易数据")
public class CurrentStockDataController {

    @Autowired
    private CurrentStockDataService currentStockDataService;

    @Autowired
    private TechnicalIndicatorService technicalIndicatorService;

    @PostMapping("/import")
    @Operation(summary = "导入CSV文件", description = "从指定目录导入所有CSV文件到current_stock_data表，只导入最近1年的数据，自动忽略已存在的记录")
    public ResponseEntity<Map<String, Object>> importCsvFiles() {
        Map<String, Object> response = new HashMap<>();
        try {
            CurrentStockDataService.ImportResult result = currentStockDataService.importAllCsvFiles();
            response.put("success", true);
            response.put("message", "导入完成");
            response.put("fileCount", result.getFileCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failCount", result.getFailCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/all")
    @Operation(summary = "查询所有当前股票数据", description = "获取最近1年内的所有股票交易数据")
    public ResponseEntity<List<CurrentStockData>> getAllCurrentStockData() {
        List<CurrentStockData> dataList = currentStockDataService.getAllCurrentStockData();
        return ResponseEntity.ok(dataList);
    }

    @GetMapping("/stock/{stockCode}")
    @Operation(summary = "根据股票代码查询", description = "查询指定股票代码的最近1年交易数据")
    public ResponseEntity<List<CurrentStockData>> getByStockCode(@PathVariable String stockCode) {
        List<CurrentStockData> dataList = currentStockDataService.getByStockCode(stockCode);
        return ResponseEntity.ok(dataList);
    }

    @GetMapping("/date-range")
    @Operation(summary = "根据日期范围查询", description = "查询指定日期范围内的股票交易数据")
    public ResponseEntity<List<CurrentStockData>> getByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<CurrentStockData> dataList = currentStockDataService.getByDateRange(startDate, endDate);
        return ResponseEntity.ok(dataList);
    }

    @DeleteMapping("/clean-old")
    @Operation(summary = "清理旧数据", description = "删除超过1年的旧数据")
    public ResponseEntity<Map<String, Object>> cleanOldData() {
        Map<String, Object> response = new HashMap<>();

        try {
            int deletedCount = currentStockDataService.cleanOldData();
            response.put("success", true);
            response.put("message", "清理成功");
            response.put("deletedCount", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "清理失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/calculate-indicators")
    @Operation(summary = "计算技术指标", description = "计算并更新所有股票的技术指标（5日线、10日线、20日线、量比）")
    public ResponseEntity<Map<String, Object>> calculateIndicators() {
        Map<String, Object> response = new HashMap<>();

        try {
            int updatedCount = technicalIndicatorService.calculateAndUpdateAllIndicators();
            response.put("success", true);
            response.put("message", "技术指标计算完成");
            response.put("updatedCount", updatedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/calculate-indicators/{stockCode}")
    @Operation(summary = "计算指定股票的技术指标", description = "计算并更新指定股票的技术指标")
    public ResponseEntity<Map<String, Object>> calculateIndicatorsByStock(@PathVariable String stockCode) {
        Map<String, Object> response = new HashMap<>();

        try {
            int updatedCount = technicalIndicatorService.calculateAndUpdateIndicators(stockCode);
            response.put("success", true);
            response.put("message", "技术指标计算完成");
            response.put("stockCode", stockCode);
            response.put("updatedCount", updatedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/calculate-ma20")
    @Operation(summary = "计算并填充所有空的MA20", description = "计算并更新所有MA20为空的记录，为每只股票计算20日移动平均线")
    public ResponseEntity<Map<String, Object>> calculateMA20() {
        Map<String, Object> response = new HashMap<>();

        try {
            int updatedCount = technicalIndicatorService.calculateAndUpdateAllIndicators();
            response.put("success", true);
            response.put("message", "MA20计算完成");
            response.put("updatedCount", updatedCount);
            response.put("description", "已计算并填充所有空的MA20值（包括MA5、MA10等其他技术指标）");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "MA20计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
