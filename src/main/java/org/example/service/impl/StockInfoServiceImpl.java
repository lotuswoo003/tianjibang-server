package org.example.service.impl;

import org.example.entity.StockInfo;
import org.example.mapper.CurrentStockDataMapper;
import org.example.mapper.StockInfoMapper;
import org.example.service.StockInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 股票基础信息服务实现
 */
@Service
public class StockInfoServiceImpl implements StockInfoService {

    private static final Logger logger = LoggerFactory.getLogger(StockInfoServiceImpl.class);

    @Autowired
    private StockInfoMapper stockInfoMapper;

    @Autowired
    private CurrentStockDataMapper currentStockDataMapper;

    // 内存缓存：股票代码 -> 股票名称
    private final Map<String, String> stockNameCache = new ConcurrentHashMap<>();

    // 内存缓存：股票代码 -> StockInfo对象
    private final Map<String, StockInfo> stockInfoCache = new ConcurrentHashMap<>();

    @Override
    public String getStockNameByCode(String stockCode) {
        if (stockCode == null || stockCode.isEmpty()) {
            return null;
        }

        // 先从缓存查找
        if (stockNameCache.containsKey(stockCode)) {
            return stockNameCache.get(stockCode);
        }

        // 从数据库查找
        StockInfo stockInfo = stockInfoMapper.findByStockCode(stockCode);
        if (stockInfo != null && stockInfo.getStockName() != null) {
            stockNameCache.put(stockCode, stockInfo.getStockName());
            return stockInfo.getStockName();
        }

        // 如果stock_info表没有，尝试从current_stock_data表查找
        String stockName = currentStockDataMapper.findStockNameByCode(stockCode);
        if (stockName != null && !stockName.isEmpty()) {
            // 找到后保存到stock_info表
            StockInfo newStockInfo = new StockInfo();
            newStockInfo.setStockCode(stockCode);
            newStockInfo.setStockName(stockName);
            stockInfoMapper.insert(newStockInfo);

            // 加入缓存
            stockNameCache.put(stockCode, stockName);
            return stockName;
        }

        return null;
    }

    @Override
    public StockInfo findByStockCode(String stockCode) {
        return stockInfoMapper.findByStockCode(stockCode);
    }

    @Override
    public StockInfo findByStockName(String stockName) {
        return stockInfoMapper.findByStockName(stockName);
    }

    @Override
    public List<StockInfo> findAll() {
        return stockInfoMapper.findAll();
    }

    @Override
    public Map<String, String> getStockCodeNameMap() {
        if (stockNameCache.isEmpty()) {
            // 初始化缓存
            List<StockInfo> allStocks = stockInfoMapper.findAll();
            for (StockInfo stock : allStocks) {
                stockNameCache.put(stock.getStockCode(), stock.getStockName());
            }
            logger.info("初始化股票名称缓存，共 {} 条记录", stockNameCache.size());
        }
        return new HashMap<>(stockNameCache);
    }

    @Override
    public boolean saveOrUpdate(StockInfo stockInfo) {
        int result = stockInfoMapper.insert(stockInfo);
        if (result > 0 && stockInfo.getStockName() != null) {
            // 更新缓存
            stockNameCache.put(stockInfo.getStockCode(), stockInfo.getStockName());
        }
        return result > 0;
    }

    @Override
    public int batchSaveOrUpdate(List<StockInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int result = stockInfoMapper.batchInsert(list);

        // 更新缓存
        for (StockInfo stock : list) {
            if (stock.getStockName() != null) {
                stockNameCache.put(stock.getStockCode(), stock.getStockName());
            }
        }

        return result;
    }

    @Override
    public int syncStockInfoFromStockData() {
        logger.info("开始从current_stock_data表同步股票信息（含行业）到stock_info表");

        // 从current_stock_data获取所有不同的股票代码、名称和行业信息
        List<Map<String, String>> stockList = currentStockDataMapper.findDistinctStockInfo();

        if (stockList.isEmpty()) {
            logger.warn("未找到任何股票数据");
            return 0;
        }

        int count = 0;
        for (Map<String, String> stock : stockList) {
            String stockCode = stock.get("stockCode");
            String stockName = stock.get("stockName");
            String industryLevel1 = stock.get("industryLevel1");
            String industryLevel2 = stock.get("industryLevel2");
            String industryLevel3 = stock.get("industryLevel3");

            if (stockCode != null && stockName != null && !stockName.isEmpty()) {
                StockInfo stockInfo = new StockInfo();
                stockInfo.setStockCode(stockCode);
                stockInfo.setStockName(stockName);
                stockInfo.setIndustryLevel1(industryLevel1);
                stockInfo.setIndustryLevel2(industryLevel2);
                stockInfo.setIndustryLevel3(industryLevel3);

                if (saveOrUpdate(stockInfo)) {
                    count++;
                }
            }
        }

        logger.info("同步完成，共处理 {} 条股票信息", count);
        return count;
    }

    @Override
    public StockInfo getStockInfoByCode(String stockCode) {
        if (stockCode == null || stockCode.isEmpty()) {
            return null;
        }

        // 先从缓存查找
        if (stockInfoCache.containsKey(stockCode)) {
            return stockInfoCache.get(stockCode);
        }

        // 从数据库查找
        StockInfo stockInfo = stockInfoMapper.findByStockCode(stockCode);
        if (stockInfo != null) {
            stockInfoCache.put(stockCode, stockInfo);
            return stockInfo;
        }

        // 如果stock_info表没有，尝试从current_stock_data表查找最新记录（包括行业信息）
        List<Map<String, String>> stockList = currentStockDataMapper.findDistinctStockInfo();
        for (Map<String, String> stock : stockList) {
            if (stockCode.equals(stock.get("stockCode"))) {
                StockInfo newStockInfo = new StockInfo();
                newStockInfo.setStockCode(stockCode);
                newStockInfo.setStockName(stock.get("stockName"));
                newStockInfo.setIndustryLevel1(stock.get("industryLevel1"));
                newStockInfo.setIndustryLevel2(stock.get("industryLevel2"));
                newStockInfo.setIndustryLevel3(stock.get("industryLevel3"));

                stockInfoMapper.insert(newStockInfo);
                stockInfoCache.put(stockCode, newStockInfo);
                return newStockInfo;
            }
        }

        return null;
    }

    @Override
    public FixStockNameResult fixAllStockNames(java.util.Date referenceDate) {
        logger.info("开始全量修复股票名称，参考日期: {}", referenceDate);
        FixStockNameResult result = new FixStockNameResult();

        try {
            // 1. 从current_stock_data获取参考日期的所有股票代码和名称
            List<Map<String, String>> referenceStockNames = currentStockDataMapper.findStockNamesByDate(referenceDate);

            if (referenceStockNames == null || referenceStockNames.isEmpty()) {
                String error = "未找到参考日期的股票数据: " + referenceDate;
                logger.error(error);
                result.getErrors().add(error);
                return result;
            }

            result.setTotalStocks(referenceStockNames.size());
            logger.info("找到 {} 只股票的参考名称", referenceStockNames.size());

            int currentStockDataUpdated = 0;
            int stockInfoUpdated = 0;

            // 2. 遍历每个股票，更新其所有记录的名称
            for (Map<String, String> stockNameMap : referenceStockNames) {
                String stockCode = stockNameMap.get("stockCode");
                String correctStockName = stockNameMap.get("stockName");

                if (stockCode == null || correctStockName == null || correctStockName.isEmpty()) {
                    continue;
                }

                try {
                    // 2.1 更新current_stock_data表中该股票的所有名称
                    int updated = currentStockDataMapper.updateStockNameByCode(stockCode, correctStockName);
                    if (updated > 0) {
                        currentStockDataUpdated++;
                        logger.debug("更新股票 {} ({}) 的 {} 条记录", stockCode, correctStockName, updated);
                    }

                    // 2.2 更新stock_info表中的股票名称
                    StockInfo existingInfo = stockInfoMapper.findByStockCode(stockCode);
                    if (existingInfo != null) {
                        // 如果名称不同，才更新
                        if (!correctStockName.equals(existingInfo.getStockName())) {
                            int infoUpdated = stockInfoMapper.updateStockName(stockCode, correctStockName);
                            if (infoUpdated > 0) {
                                stockInfoUpdated++;
                                logger.info("修复股票名称: {} ({} -> {})",
                                    stockCode, existingInfo.getStockName(), correctStockName);
                            }
                        }
                    } else {
                        // stock_info表中不存在该股票，创建新记录
                        StockInfo newInfo = new StockInfo();
                        newInfo.setStockCode(stockCode);
                        newInfo.setStockName(correctStockName);
                        stockInfoMapper.insert(newInfo);
                        stockInfoUpdated++;
                        logger.info("新增股票信息: {} ({})", stockCode, correctStockName);
                    }

                    // 3. 更新缓存
                    stockNameCache.put(stockCode, correctStockName);
                    if (existingInfo != null) {
                        existingInfo.setStockName(correctStockName);
                        stockInfoCache.put(stockCode, existingInfo);
                    }

                } catch (Exception e) {
                    String error = String.format("处理股票 %s 失败: %s", stockCode, e.getMessage());
                    logger.error(error, e);
                    result.getErrors().add(error);
                }
            }

            result.setUpdatedCurrentStockData(currentStockDataUpdated);
            result.setUpdatedStockInfo(stockInfoUpdated);

            logger.info("股票名称修复完成: current_stock_data更新{}只股票, stock_info更新{}只股票",
                currentStockDataUpdated, stockInfoUpdated);

        } catch (Exception e) {
            String error = "全量修复股票名称失败: " + e.getMessage();
            logger.error(error, e);
            result.getErrors().add(error);
        }

        return result;
    }
}
