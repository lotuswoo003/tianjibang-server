package org.example.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.example.entity.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV文件导入服务
 */
@Service
public class CsvImportService {

    private static final Logger logger = LoggerFactory.getLogger(CsvImportService.class);

    @Autowired
    private StockDataService stockDataService;

    private static final String CSV_DIRECTORY = "/Users/lynn/IdeaProject/study/stock_data";
    private static final int BATCH_SIZE = 1000; // 批量插入大小

    /**
     * 导入指定目录下的所有CSV文件
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importAllCsvFiles() {
        File dir = new File(CSV_DIRECTORY);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException("目录不存在: " + CSV_DIRECTORY);
        }

        File[] csvFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null || csvFiles.length == 0) {
            return new ImportResult(0, 0, 0, "没有找到CSV文件");
        }

        ImportResult totalResult = new ImportResult();
        for (File csvFile : csvFiles) {
            try {
                logger.info("开始导入文件: {}", csvFile.getName());
                ImportResult result = importCsvFile(csvFile);
                totalResult.add(result);
                logger.info("文件导入完成: {}, 成功: {}, 失败: {}",
                    csvFile.getName(), result.getSuccessCount(), result.getFailCount());
            } catch (Exception e) {
                logger.error("导入文件失败: {}, 错误: {}", csvFile.getName(), e.getMessage(), e);
                totalResult.incrementFail();
            }
        }

        return totalResult;
    }

    /**
     * 导入单个CSV文件
     */
    public ImportResult importCsvFile(File csvFile) throws Exception {
        ImportResult result = new ImportResult();
        List<StockData> batchList = new ArrayList<>();

        // 使用GBK编码读取CSV文件
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile), Charset.forName("GBK")));
             CSVParser csvParser = new CSVParser(reader,
                CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build())) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            for (CSVRecord record : csvParser) {
                try {
                    StockData stockData = parseRecord(record, sdf);
                    if (stockData != null) {
                        batchList.add(stockData);

                        // 批量插入
                        if (batchList.size() >= BATCH_SIZE) {
                            boolean success = stockDataService.batchSave(batchList);
                            if (success) {
                                result.addSuccess(batchList.size());
                            } else {
                                result.addFail(batchList.size());
                            }
                            batchList.clear();
                        }
                    }
                } catch (Exception e) {
                    logger.warn("解析记录失败, 行号: {}, 错误: {}", record.getRecordNumber(), e.getMessage());
                    result.incrementFail();
                }
            }

            // 插入剩余数据
            if (!batchList.isEmpty()) {
                boolean success = stockDataService.batchSave(batchList);
                if (success) {
                    result.addSuccess(batchList.size());
                } else {
                    result.addFail(batchList.size());
                }
            }
        }

        return result;
    }

    /**
     * 解析CSV记录为StockData对象
     */
    private StockData parseRecord(CSVRecord record, SimpleDateFormat sdf) throws Exception {
        StockData stockData = new StockData();

        // 股票代码
        stockData.setStockCode(record.get("股票代码"));

        // 股票名称
        stockData.setStockName(record.get("股票名称"));

        // 交易日期
        String tradeDateStr = record.get("交易日期");
        if (tradeDateStr != null && !tradeDateStr.isEmpty()) {
            stockData.setTradeDate(sdf.parse(tradeDateStr));
        }

        // 开盘价
        stockData.setOpenPrice(parseBigDecimal(record.get("开盘价")));

        // 最高价
        stockData.setHighPrice(parseBigDecimal(record.get("最高价")));

        // 最低价
        stockData.setLowPrice(parseBigDecimal(record.get("最低价")));

        // 收盘价
        stockData.setClosePrice(parseBigDecimal(record.get("收盘价")));

        // 成交量
        stockData.setVolume(parseLong(record.get("成交量")));

        // 成交额
        stockData.setTurnover(parseBigDecimal(record.get("成交额")));

        // 流通市值
        stockData.setFloatMarketValue(parseBigDecimal(record.get("流通市值")));

        // 总市值
        stockData.setTotalMarketValue(parseBigDecimal(record.get("总市值")));

        // 申万行业
        stockData.setIndustryLevel1(record.get("新版申万一级行业名称"));
        stockData.setIndustryLevel2(record.get("新版申万二级行业名称"));
        stockData.setIndustryLevel3(record.get("新版申万三级行业名称"));

        return stockData;
    }

    /**
     * 解析BigDecimal，处理空值
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析Long，处理空值
     */
    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.split("\\.")[0]); // 处理科学计数法
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 导入结果类
     */
    public static class ImportResult {
        private int fileCount;
        private int successCount;
        private int failCount;
        private String message;

        public ImportResult() {
            this.fileCount = 0;
            this.successCount = 0;
            this.failCount = 0;
            this.message = "";
        }

        public ImportResult(int fileCount, int successCount, int failCount, String message) {
            this.fileCount = fileCount;
            this.successCount = successCount;
            this.failCount = failCount;
            this.message = message;
        }

        public void add(ImportResult other) {
            this.fileCount++;
            this.successCount += other.successCount;
            this.failCount += other.failCount;
        }

        public void addSuccess(int count) {
            this.successCount += count;
        }

        public void addFail(int count) {
            this.failCount += count;
        }

        public void incrementFail() {
            this.failCount++;
        }

        // Getters and Setters
        public int getFileCount() {
            return fileCount;
        }

        public void setFileCount(int fileCount) {
            this.fileCount = fileCount;
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
    }
}
