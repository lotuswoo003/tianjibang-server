package org.example.service;

import org.example.entity.StockData;

import java.util.Date;
import java.util.List;

/**
 * 股票数据服务接口
 */
public interface StockDataService {

    /**
     * 根据ID查询股票数据
     */
    StockData getById(Long id);

    /**
     * 根据股票代码查询最新数据
     */
    StockData getByStockCode(String stockCode);

    /**
     * 查询所有股票数据
     */
    List<StockData> getAllStockData();

    /**
     * 根据股票代码和日期查询
     */
    StockData getByStockCodeAndDate(String stockCode, Date tradeDate);

    /**
     * 根据日期范围查询
     */
    List<StockData> getByDateRange(String stockCode, Date startDate, Date endDate);

    /**
     * 根据行业查询
     */
    List<StockData> getByIndustry(String industry);

    /**
     * 保存股票数据
     */
    boolean save(StockData stockData);

    /**
     * 批量保存股票数据
     */
    boolean batchSave(List<StockData> stockDataList);

    /**
     * 更新股票数据
     */
    boolean update(StockData stockData);

    /**
     * 根据ID删除
     */
    boolean deleteById(Long id);

    /**
     * 根据股票代码删除
     */
    boolean deleteByStockCode(String stockCode);
}
