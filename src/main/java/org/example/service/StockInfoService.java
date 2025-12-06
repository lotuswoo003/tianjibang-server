package org.example.service;

import org.example.entity.StockInfo;

import java.util.List;
import java.util.Map;

/**
 * 股票基础信息服务接口
 */
public interface StockInfoService {

    /**
     * 根据股票代码获取股票名称
     */
    String getStockNameByCode(String stockCode);

    /**
     * 根据股票代码查询股票信息
     */
    StockInfo findByStockCode(String stockCode);

    /**
     * 根据股票名称查询股票信息
     */
    StockInfo findByStockName(String stockName);

    /**
     * 获取所有股票信息
     */
    List<StockInfo> findAll();

    /**
     * 获取股票代码到名称的映射（用于缓存）
     */
    Map<String, String> getStockCodeNameMap();

    /**
     * 保存或更新股票信息
     */
    boolean saveOrUpdate(StockInfo stockInfo);

    /**
     * 批量保存或更新
     */
    int batchSaveOrUpdate(List<StockInfo> list);

    /**
     * 从stock_data表同步股票名称和行业信息到stock_info表
     */
    int syncStockInfoFromStockData();

    /**
     * 根据股票代码获取完整股票信息（包括行业）
     */
    StockInfo getStockInfoByCode(String stockCode);

    /**
     * 全量修复股票名称
     * 以指定日期（如2025-09-24）的股票名称为准，更新current_stock_data和stock_info表中的所有股票名称
     * @param referenceDate 参考日期（格式：yyyy-MM-dd），该日期的股票名称被认为是正确的
     * @return 修复结果统计
     */
    FixStockNameResult fixAllStockNames(java.util.Date referenceDate);

    /**
     * 股票名称修复结果
     */
    class FixStockNameResult {
        private int totalStocks;
        private int updatedCurrentStockData;
        private int updatedStockInfo;
        private java.util.List<String> errors;

        public FixStockNameResult() {
            this.errors = new java.util.ArrayList<>();
        }

        public int getTotalStocks() {
            return totalStocks;
        }

        public void setTotalStocks(int totalStocks) {
            this.totalStocks = totalStocks;
        }

        public int getUpdatedCurrentStockData() {
            return updatedCurrentStockData;
        }

        public void setUpdatedCurrentStockData(int updatedCurrentStockData) {
            this.updatedCurrentStockData = updatedCurrentStockData;
        }

        public int getUpdatedStockInfo() {
            return updatedStockInfo;
        }

        public void setUpdatedStockInfo(int updatedStockInfo) {
            this.updatedStockInfo = updatedStockInfo;
        }

        public java.util.List<String> getErrors() {
            return errors;
        }

        public void setErrors(java.util.List<String> errors) {
            this.errors = errors;
        }
    }
}
