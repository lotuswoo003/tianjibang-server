package org.example.service;

import org.example.entity.CurrentStockData;

import java.util.Date;
import java.util.List;

public interface CurrentStockDataService {

    /**
     * 导入结果类
     */
    class ImportResult {
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

        public int getFileCount() {
            return fileCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 从CSV目录导入最近1年的数据（忽略重复记录）
     */
    ImportResult importAllCsvFiles();

    /**
     * 查询所有当前股票数据
     */
    List<CurrentStockData> getAllCurrentStockData();

    /**
     * 根据股票代码查询
     */
    List<CurrentStockData> getByStockCode(String stockCode);

    /**
     * 根据日期范围查询
     */
    List<CurrentStockData> getByDateRange(Date startDate, Date endDate);

    /**
     * 清理超过1年的旧数据
     */
    int cleanOldData();
}
