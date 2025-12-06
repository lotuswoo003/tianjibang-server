package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.service.StockInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 股票基础信息控制器
 */
@RestController
@RequestMapping("/api/stock-info")
@Tag(name = "股票基础信息管理", description = "股票名称、行业信息等基础数据管理")
public class StockInfoController {

    @Autowired
    private StockInfoService stockInfoService;

    @PostMapping("/sync-from-current-data")
    @Operation(summary = "从current_stock_data同步股票信息", description = "从current_stock_data表同步股票名称和行业信息到stock_info表")
    public ResponseEntity<Map<String, Object>> syncStockInfo() {
        Map<String, Object> response = new HashMap<>();

        try {
            int count = stockInfoService.syncStockInfoFromStockData();
            response.put("success", true);
            response.put("message", "同步完成");
            response.put("syncedCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "同步失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/fix-stock-names")
    @Operation(summary = "全量修复股票名称",
               description = "以指定日期（如2025-09-24）的股票名称为准，批量修复current_stock_data和stock_info表中的所有股票名称。用于处理股票改名问题（如ST峡创改名为海峡创新）")
    public ResponseEntity<Map<String, Object>> fixStockNames(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "参考日期（格式：yyyy-MM-dd），该日期的股票名称被认为是正确的", example = "2025-09-24")
            Date referenceDate) {
        Map<String, Object> response = new HashMap<>();

        try {
            StockInfoService.FixStockNameResult result = stockInfoService.fixAllStockNames(referenceDate);

            response.put("success", true);
            response.put("message", "股票名称修复完成");
            response.put("referenceDate", referenceDate);
            response.put("totalStocks", result.getTotalStocks());
            response.put("updatedCurrentStockData", result.getUpdatedCurrentStockData());
            response.put("updatedStockInfo", result.getUpdatedStockInfo());

            if (!result.getErrors().isEmpty()) {
                response.put("errors", result.getErrors());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "修复失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
