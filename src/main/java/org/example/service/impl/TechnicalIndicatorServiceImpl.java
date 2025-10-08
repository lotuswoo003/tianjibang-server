package org.example.service.impl;

import org.example.entity.CurrentStockData;
import org.example.mapper.CurrentStockDataMapper;
import org.example.service.TechnicalIndicatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class TechnicalIndicatorServiceImpl implements TechnicalIndicatorService {

    private static final Logger logger = LoggerFactory.getLogger(TechnicalIndicatorServiceImpl.class);

    @Autowired
    private CurrentStockDataMapper currentStockDataMapper;

    @Override
    @Transactional
    public int calculateAndUpdateAllIndicators() {
        logger.info("开始计算所有股票的技术指标");

        List<String> stockCodes = currentStockDataMapper.selectAllStockCodes();
        int totalUpdated = 0;

        for (String stockCode : stockCodes) {
            try {
                int updated = calculateAndUpdateIndicators(stockCode);
                totalUpdated += updated;
            } catch (Exception e) {
                logger.error("计算股票 {} 的技术指标失败: {}", stockCode, e.getMessage(), e);
            }
        }

        logger.info("技术指标计算完成，共更新 {} 条记录", totalUpdated);
        return totalUpdated;
    }

    @Override
    @Transactional
    public int calculateAndUpdateIndicators(String stockCode) {
        // 获取该股票的所有数据（按日期升序）
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1); // 获取1年的数据以确保有足够的数据计算均线
        Date oneYearAgo = calendar.getTime();
        Date today = new Date();

        List<CurrentStockData> dataList = currentStockDataMapper
                .selectByStockCodeAndDateRange(stockCode, oneYearAgo, today);

        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;

        // 从第20天开始计算（确保有足够的数据计算20日均线）
        for (int i = 0; i < dataList.size(); i++) {
            CurrentStockData currentData = dataList.get(i);

            // 计算5日均线
            BigDecimal ma5 = null;
            if (i >= 4) {
                ma5 = calculateMA(dataList, i, 5);
            }

            // 计算10日均线
            BigDecimal ma10 = null;
            if (i >= 9) {
                ma10 = calculateMA(dataList, i, 10);
            }

            // 计算20日均线
            BigDecimal ma20 = null;
            if (i >= 19) {
                ma20 = calculateMA(dataList, i, 20);
            }

            // 计算量比
            BigDecimal volumeRatio = null;
            if (i >= 5 && currentData.getVolume() != null) {
                volumeRatio = calculateVolumeRatio(dataList, i);
            }

            // 更新数据库
            if (ma5 != null || ma10 != null || ma20 != null || volumeRatio != null) {
                currentStockDataMapper.updateTechnicalIndicators(
                        currentData.getId(), ma5, ma10, ma20, volumeRatio);
                updatedCount++;
            }
        }

        return updatedCount;
    }

    /**
     * 计算移动平均线（MA）
     * @param dataList 数据列表
     * @param currentIndex 当前索引
     * @param period 周期（5、10、20）
     * @return MA值
     */
    private BigDecimal calculateMA(List<CurrentStockData> dataList, int currentIndex, int period) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            if (i >= 0) {
                BigDecimal closePrice = dataList.get(i).getClosePrice();
                if (closePrice != null) {
                    sum = sum.add(closePrice);
                    count++;
                }
            }
        }

        if (count == 0) {
            return null;
        }

        return sum.divide(new BigDecimal(count), 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算量比
     * 量比 = 当前成交量 / 前5日平均成交量
     * @param dataList 数据列表
     * @param currentIndex 当前索引
     * @return 量比
     */
    private BigDecimal calculateVolumeRatio(List<CurrentStockData> dataList, int currentIndex) {
        CurrentStockData currentData = dataList.get(currentIndex);
        Long currentVolume = currentData.getVolume();

        if (currentVolume == null || currentVolume == 0) {
            return null;
        }

        // 计算前5日平均成交量
        long sumVolume = 0;
        int count = 0;

        for (int i = currentIndex - 5; i < currentIndex; i++) {
            if (i >= 0) {
                Long volume = dataList.get(i).getVolume();
                if (volume != null) {
                    sumVolume += volume;
                    count++;
                }
            }
        }

        if (count == 0 || sumVolume == 0) {
            return null;
        }

        double avgVolume = (double) sumVolume / count;
        double ratio = currentVolume / avgVolume;

        return new BigDecimal(ratio).setScale(4, RoundingMode.HALF_UP);
    }
}
