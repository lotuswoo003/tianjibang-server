package org.example.service;

import org.example.entity.CurrentStockData;
import org.example.mapper.CurrentStockDataMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 增量数据导入服务
 */
@Service
public class IncrementalDataImportService {

    private static final Logger logger = LoggerFactory.getLogger(IncrementalDataImportService.class);

    @Autowired
    private CurrentStockDataMapper currentStockDataMapper;

    @Autowired
    private TechnicalIndicatorService technicalIndicatorService;

    @Autowired
    private StockInfoService stockInfoService;

    @Value("${app.data-import.path}")
    private String dataImportPath;

    @Value("${app.data-import.enabled:true}")
    private boolean importEnabled;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 定时任务：每天凌晨2点执行
     * 可通过 app.data-import.enabled=false 禁用
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledImport() {
        if (!importEnabled) {
            logger.info("定时任务已禁用");
            return;
        }

        logger.info("开始执行定时增量数据导入任务");
        try {
            ImportResult result = importIncrementalData();
            logger.info("定时任务完成: 成功导入{}条记录，失败{}条", result.getSuccessCount(), result.getFailCount());
        } catch (Exception e) {
            logger.error("定时任务执行失败", e);
        }
    }

    /**
     * 手动触发增量数据导入
     */
    public ImportResult importIncrementalData() {
        ImportResult result = new ImportResult();
        File dataDir = new File(dataImportPath);

        if (!dataDir.exists() || !dataDir.isDirectory()) {
            logger.error("数据目录不存在: {}", dataImportPath);
            result.setSuccess(false);
            result.setMessage("数据目录不存在: " + dataImportPath);
            return result;
        }

        // 获取所有CSV文件，文件名格式: daily_yyyy-MM-dd.csv
        File[] csvFiles = dataDir.listFiles((dir, name) ->
            name.toLowerCase().startsWith("daily_") && name.toLowerCase().endsWith(".csv"));

        if (csvFiles == null || csvFiles.length == 0) {
            logger.warn("数据目录中没有CSV文件: {}", dataImportPath);
            result.setSuccess(true);
            result.setMessage("没有待导入的数据");
            return result;
        }

        // 按文件名排序
        Arrays.sort(csvFiles, Comparator.comparing(File::getName));

        int totalSuccess = 0;
        int totalFail = 0;
        List<String> processedDates = new ArrayList<>();

        // 逐个处理CSV文件
        for (File csvFile : csvFiles) {
            String fileName = csvFile.getName();
            logger.info("开始处理文件: {}", fileName);

            try {
                // 从文件名中提取日期: daily_2025-09-25.csv -> 2025-09-25
                String dateStr = fileName.replace("daily_", "").replace(".csv", "");
                Date tradeDate = dateFormat.parse(dateStr);

                int imported = importCsvFile(csvFile, tradeDate);
                totalSuccess += imported;
                processedDates.add(dateStr);
                logger.info("文件 {} 导入完成，成功 {} 条记录", fileName, imported);

            } catch (ParseException e) {
                logger.error("文件名日期格式错误: {}", fileName);
                totalFail++;
            } catch (Exception e) {
                logger.error("处理文件 {} 失败", fileName, e);
                totalFail++;
            }
        }

        // 导入完成后计算技术指标
        if (totalSuccess > 0) {
            logger.info("开始计算技术指标...");
            try {
                int updatedCount = technicalIndicatorService.calculateAndUpdateAllIndicators();
                logger.info("技术指标计算完成，更新 {} 条记录", updatedCount);
            } catch (Exception e) {
                logger.error("计算技术指标失败", e);
            }
        }

        result.setSuccess(true);
        result.setSuccessCount(totalSuccess);
        result.setFailCount(totalFail);
        result.setProcessedDates(processedDates);
        result.setMessage(String.format("导入完成: 成功%d条，失败%d条，处理%d个日期",
                totalSuccess, totalFail, processedDates.size()));

        return result;
    }

    /**
     * 导入单个CSV文件
     */
    private int importCsvFile(File csvFile, Date tradeDate) throws IOException {
        int totalImported = 0;
        List<CurrentStockData> batchData = new ArrayList<>();

        logger.info("开始读取文件: {}", csvFile.getName());

        try (FileReader reader = new FileReader(csvFile, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : csvParser) {
                try {
                    CurrentStockData data = parseCSVRecord(record, tradeDate);
                    if (data != null) {
                        batchData.add(data);

                        // 每1000条批量插入一次
                        if (batchData.size() >= 1000) {
                            int inserted = currentStockDataMapper.insertBatch(batchData);
                            totalImported += inserted;
                            batchData.clear();
                        }
                    }
                } catch (Exception e) {
                    logger.error("解析CSV记录失败: {}", record, e);
                }
            }

        } catch (IOException e) {
            logger.error("读取CSV文件失败: {}", csvFile.getName(), e);
            throw e;
        }

        // 插入剩余数据
        if (!batchData.isEmpty()) {
            int inserted = currentStockDataMapper.insertBatch(batchData);
            totalImported += inserted;
        }

        return totalImported;
    }

    /**
     * 解析CSV记录为CurrentStockData对象
     * CSV列: 日期,股票代码,开盘,收盘,最高,最低,成交量,成交额,振幅,涨跌幅,涨跌额,换手率
     */
    private CurrentStockData parseCSVRecord(CSVRecord record, Date tradeDate) {
        try {
            CurrentStockData data = new CurrentStockData();

            // 基本信息
            String stockCode = record.get("股票代码");
            data.setStockCode(stockCode);

            // 从stock_info表获取股票完整信息（名称和行业）
            org.example.entity.StockInfo stockInfo = stockInfoService.getStockInfoByCode(stockCode);
            if (stockInfo != null) {
                data.setStockName(stockInfo.getStockName());
                data.setIndustryLevel1(stockInfo.getIndustryLevel1());
                data.setIndustryLevel2(stockInfo.getIndustryLevel2());
                data.setIndustryLevel3(stockInfo.getIndustryLevel3());
            } else {
                // 如果没有找到，只设置股票名称为null，行业信息也为null
                data.setStockName(null);
                data.setIndustryLevel1(null);
                data.setIndustryLevel2(null);
                data.setIndustryLevel3(null);
            }
            data.setTradeDate(tradeDate);

            // 价格数据
            data.setOpenPrice(parseBigDecimal(record.get("开盘")));
            data.setHighPrice(parseBigDecimal(record.get("最高")));
            data.setLowPrice(parseBigDecimal(record.get("最低")));
            data.setClosePrice(parseBigDecimal(record.get("收盘")));

            // 成交数据
            data.setVolume(parseLong(record.get("成交量")));
            data.setTurnover(parseBigDecimal(record.get("成交额")));

            // 注意：CSV中没有流通市值和总市值，需要计算或从其他源获取
            // 可以根据换手率反推：流通市值 = 成交额 / 换手率
            BigDecimal turnoverRate = parseBigDecimal(record.get("换手率"));
            if (turnoverRate != null && turnoverRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal turnoverAmount = data.getTurnover();
                if (turnoverAmount != null) {
                    // 换手率是百分比，需要除以100
                    BigDecimal floatMarketValue = turnoverAmount.divide(
                            turnoverRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP),
                            2, RoundingMode.HALF_UP);
                    data.setFloatMarketValue(floatMarketValue);
                }
            }

            return data;
        } catch (Exception e) {
            logger.error("解析记录失败: {}", record, e);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 导入结果
     */
    public static class ImportResult {
        private boolean success;
        private int successCount;
        private int failCount;
        private String message;
        private List<String> processedDates;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<String> getProcessedDates() {
            return processedDates;
        }

        public void setProcessedDates(List<String> processedDates) {
            this.processedDates = processedDates;
        }
    }
}
