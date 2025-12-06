package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.service.IncrementalDataImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 定时任务控制器
 */
@RestController
@RequestMapping("/api/scheduled-task")
@Tag(name = "定时任务管理", description = "手动触发定时任务")
public class ScheduledTaskController {

    @Autowired
    private IncrementalDataImportService incrementalDataImportService;

    @PostMapping("/import-incremental-data")
    @Operation(summary = "手动触发增量数据导入",
               description = "从配置的目录读取按日期分类的CSV文件并导入到数据库，导入后自动计算技术指标")
    public ResponseEntity<Map<String, Object>> importIncrementalData() {
        Map<String, Object> response = new HashMap<>();

        try {
            IncrementalDataImportService.ImportResult result =
                    incrementalDataImportService.importIncrementalData();

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("successCount", result.getSuccessCount());
            response.put("failCount", result.getFailCount());
            response.put("processedDates", result.getProcessedDates());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
