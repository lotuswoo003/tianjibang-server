package org.example.service.impl;

import org.example.entity.StockData;
import org.example.mapper.StockDataMapper;
import org.example.service.StockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 股票数据服务实现类
 */
@Service
public class StockDataServiceImpl implements StockDataService {

    @Autowired
    private StockDataMapper stockDataMapper;

    @Override
    public StockData getById(Long id) {
        return stockDataMapper.findById(id);
    }

    @Override
    public StockData getByStockCode(String stockCode) {
        return stockDataMapper.findByStockCode(stockCode);
    }

    @Override
    public List<StockData> getAllStockData() {
        return stockDataMapper.findAll();
    }

    @Override
    public StockData getByStockCodeAndDate(String stockCode, Date tradeDate) {
        return stockDataMapper.findByStockCodeAndDate(stockCode, tradeDate);
    }

    @Override
    public List<StockData> getByDateRange(String stockCode, Date startDate, Date endDate) {
        return stockDataMapper.findByDateRange(stockCode, startDate, endDate);
    }

    @Override
    public List<StockData> getByIndustry(String industry) {
        return stockDataMapper.findByIndustry(industry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(StockData stockData) {
        return stockDataMapper.insert(stockData) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(List<StockData> stockDataList) {
        if (stockDataList == null || stockDataList.isEmpty()) {
            return false;
        }
        return stockDataMapper.batchInsert(stockDataList) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(StockData stockData) {
        return stockDataMapper.update(stockData) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        return stockDataMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByStockCode(String stockCode) {
        return stockDataMapper.deleteByStockCode(stockCode) > 0;
    }
}
