package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.entity.StockData;
import org.example.service.CsvImportService;
import org.example.service.StockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 股票数据控制器
 */
@RestController
@RequestMapping("/api/stock")
@Tag(name = "股票数据接口", description = "股票数据的CRUD和导入功能")
public class StockDataController {

    @Autowired
    private StockDataService stockDataService;

    @Autowired
    private CsvImportService csvImportService;

    /**
     * 导入CSV文件
     */
    @PostMapping("/import")
    @Operation(summary = "导入CSV文件", description = "从指定目录导入所有CSV文件到数据库")
    public ResponseEntity<Map<String, Object>> importCsvFiles() {
        Map<String, Object> response = new HashMap<>();
        try {
            CsvImportService.ImportResult result = csvImportService.importAllCsvFiles();
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

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询股票数据")
    public ResponseEntity<StockData> getById(@PathVariable Long id) {
        StockData stockData = stockDataService.getById(id);
        if (stockData != null) {
            return ResponseEntity.ok(stockData);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 根据股票代码查询
     */
    @GetMapping("/code/{stockCode}")
    @Operation(summary = "根据股票代码查询最新数据")
    public ResponseEntity<StockData> getByStockCode(@PathVariable String stockCode) {
        StockData stockData = stockDataService.getByStockCode(stockCode);
        if (stockData != null) {
            return ResponseEntity.ok(stockData);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 查询所有数据
     */
    @GetMapping("/all")
    @Operation(summary = "查询所有股票数据")
    public ResponseEntity<List<StockData>> getAll() {
        List<StockData> list = stockDataService.getAllStockData();
        return ResponseEntity.ok(list);
    }

    /**
     * 根据日期范围查询
     */
    @GetMapping("/range")
    @Operation(summary = "根据日期范围查询")
    public ResponseEntity<List<StockData>> getByDateRange(
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<StockData> list = stockDataService.getByDateRange(stockCode, startDate, endDate);
        return ResponseEntity.ok(list);
    }

    /**
     * 根据行业查询
     */
    @GetMapping("/industry/{industry}")
    @Operation(summary = "根据行业查询")
    public ResponseEntity<List<StockData>> getByIndustry(@PathVariable String industry) {
        List<StockData> list = stockDataService.getByIndustry(industry);
        return ResponseEntity.ok(list);
    }

    /**
     * 创建股票数据
     */
    @PostMapping
    @Operation(summary = "创建股票数据")
    public ResponseEntity<Map<String, Object>> create(@RequestBody StockData stockData) {
        Map<String, Object> response = new HashMap<>();
        boolean success = stockDataService.save(stockData);
        response.put("success", success);
        response.put("message", success ? "创建成功" : "创建失败");
        return ResponseEntity.ok(response);
    }

    /**
     * 更新股票数据
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新股票数据")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody StockData stockData) {
        Map<String, Object> response = new HashMap<>();
        stockData.setId(id);
        boolean success = stockDataService.update(stockData);
        response.put("success", success);
        response.put("message", success ? "更新成功" : "更新失败");
        return ResponseEntity.ok(response);
    }

    /**
     * 删除股票数据
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除股票数据")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        boolean success = stockDataService.deleteById(id);
        response.put("success", success);
        response.put("message", success ? "删除成功" : "删除失败");
        return ResponseEntity.ok(response);
    }
}
