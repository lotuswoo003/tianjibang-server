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
        // 1. 查询未计算指标的记录
        List<CurrentStockData> uncalculatedList = currentStockDataMapper
                .selectUncalculatedByStockCode(stockCode);

        if (uncalculatedList == null || uncalculatedList.isEmpty()) {
            logger.debug("股票 {} 没有需要计算的新数据", stockCode);
            return 0;
        }

        logger.info("股票 {} 有 {} 条记录需要计算技术指标", stockCode, uncalculatedList.size());

        // 2. 获取最早需要计算的日期，向前取20天以保证有足够数据
        Date earliestDate = uncalculatedList.get(0).getTradeDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(earliestDate);
        calendar.add(Calendar.DAY_OF_MONTH, -20);
        Date startDate = calendar.getTime();

        // 3. 获取从startDate到现在的所有数据（用于计算均线）
        Date today = new Date();
        List<CurrentStockData> allDataList = currentStockDataMapper
                .selectByStockCodeAndDateRange(stockCode, startDate, today);

        if (allDataList == null || allDataList.isEmpty()) {
            logger.warn("股票 {} 没有历史数据用于计算", stockCode);
            return 0;
        }

        // 4. 创建一个Map来快速定位未计算记录
        java.util.Set<Long> uncalculatedIds = new java.util.HashSet<>();
        for (CurrentStockData data : uncalculatedList) {
            uncalculatedIds.add(data.getId());
        }

        int updatedCount = 0;

        // 5. 遍历所有数据，只更新需要计算的记录
        for (int i = 0; i < allDataList.size(); i++) {
            CurrentStockData currentData = allDataList.get(i);

            // 跳过已经计算过的记录
            if (!uncalculatedIds.contains(currentData.getId())) {
                continue;
            }

            // 计算5日均线
            BigDecimal ma5 = null;
            if (i >= 4) {
                ma5 = calculateMA(allDataList, i, 5);
            }

            // 计算10日均线
            BigDecimal ma10 = null;
            if (i >= 9) {
                ma10 = calculateMA(allDataList, i, 10);
            }

            // 计算20日均线
            BigDecimal ma20 = null;
            if (i >= 19) {
                ma20 = calculateMA(allDataList, i, 20);
            }

            // 计算量比
            BigDecimal volumeRatio = null;
            if (i >= 5 && currentData.getTurnover() != null) {
                volumeRatio = calculateVolumeRatio(allDataList, i);
            }

            // 计算换手率（成交量/流通市值*100）
            BigDecimal turnoverRate = calculateTurnoverRate(currentData);

            // 更新数据库
            if (ma5 != null || ma10 != null || ma20 != null || volumeRatio != null || turnoverRate != null) {
                currentData.setMa5(ma5);
                currentData.setMa10(ma10);
                currentData.setMa20(ma20);
                currentData.setVolumeRatio(volumeRatio);
                currentData.setTurnoverRate(turnoverRate);
                currentStockDataMapper.updateTechnicalIndicators(currentData);
                updatedCount++;
            }
        }

        logger.info("股票 {} 完成技术指标计算，更新 {} 条记录", stockCode, updatedCount);
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
        Long currentVolume = currentData.getTurnover().longValue();

        if (currentVolume == null || currentVolume == 0) {
            return null;
        }

        // 计算前5日平均成交量
        long sumVolume = 0;
        int count = 0;

        for (int i = currentIndex - 5; i < currentIndex; i++) {
            if (i >= 0) {
                Long volume = dataList.get(i).getTurnover().longValue();
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

    /**
     * 计算换手率
     * 换手率(%) = (成交量 / 流通市值) × 股价 × 100
     * 或简化为：换手率(%) = (成交量 / 流通股本) × 100
     * 这里使用：换手率(%) = (成交量 × 收盘价 / 流通市值) × 100
     *
     * @param data 股票数据
     * @return 换手率
     */
    private BigDecimal calculateTurnoverRate(CurrentStockData data) {
        if (data.getTurnover() == null || data.getFloatMarketValue() == null || data.getFloatMarketValue().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // 换手率 = (成交量 × 收盘价 / 流通市值) × 100
        // 但流通市值已经是金额，成交量是股数
        // 所以：换手率 = (成交量 × 收盘价 / 流通市值) × 100


        BigDecimal volumeValue =data.getTurnover();
        BigDecimal rate = volumeValue.divide(data.getFloatMarketValue(), 6, RoundingMode.HALF_UP)
                                     .multiply(new BigDecimal("100"));

        return rate.setScale(4, RoundingMode.HALF_UP);
    }
}
