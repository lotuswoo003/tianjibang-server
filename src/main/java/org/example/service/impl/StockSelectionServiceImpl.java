package org.example.service.impl;

import org.example.dto.StockSelectionResult;
import org.example.entity.CurrentStockData;
import org.example.enums.StockSelectionStrategy;
import org.example.mapper.CurrentStockDataMapper;
import org.example.service.StockSelectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class StockSelectionServiceImpl implements StockSelectionService {

    private static final Logger logger = LoggerFactory.getLogger(StockSelectionServiceImpl.class);

    @Autowired
    private CurrentStockDataMapper currentStockDataMapper;

    // 涨停阈值：9.9%（考虑到精度问题）
    private static final BigDecimal LIMIT_UP_THRESHOLD = new BigDecimal("0.099");

    @Override
    public List<StockSelectionResult> selectStocks(String strategyCode) {
        StockSelectionStrategy strategy = StockSelectionStrategy.fromCode(strategyCode);
        return selectStocks(strategy);
    }

    @Override
    public List<StockSelectionResult> selectStocks(StockSelectionStrategy strategy) {
        switch (strategy) {
            case CONSECUTIVE_LIMIT_UP:
                return findConsecutiveLimitUpStocks();
            case HISTORY_TWO_WAVES:
                return findHistoryTwoWavesStocks();
            case BOTTOM_VOLUME:
            case MA_BULLISH:
                throw new UnsupportedOperationException("策略 " + strategy.getDescription() + " 尚未实现");
            default:
                throw new IllegalArgumentException("未知的策略: " + strategy);
        }
    }

    @Override
    public List<StockSelectionResult> findConsecutiveLimitUpStocks() {
        List<StockSelectionResult> results = new ArrayList<>();

        // 计算半年前的日期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -6);
        Date sixMonthsAgo = calendar.getTime();
        Date today = new Date();

        // 获取所有股票代码
        List<String> stockCodes = currentStockDataMapper.selectAllStockCodes();
        logger.info("开始分析 {} 只股票的连续涨停情况", stockCodes.size());

        for (String stockCode : stockCodes) {
            try {
                // 查询该股票最近半年的交易数据
                List<CurrentStockData> stockDataList = currentStockDataMapper
                        .selectByStockCodeAndDateRange(stockCode, sixMonthsAgo, today);

                if (stockDataList == null || stockDataList.size() < 2) {
                    continue;
                }

                // 检查是否有连续涨停
                ConsecutiveLimitUpInfo limitUpInfo = checkConsecutiveLimitUp(stockDataList);

                if (limitUpInfo.hasConsecutiveLimitUp) {
                    CurrentStockData latestData = stockDataList.get(stockDataList.size() - 1);

                    StockSelectionResult result = new StockSelectionResult();
                    result.setStockCode(stockCode);
                    result.setStockName(latestData.getStockName());
                    result.setLatestTradeDate(latestData.getTradeDate());
                    result.setLatestClosePrice(latestData.getClosePrice());
                    result.setIndustryLevel1(latestData.getIndustryLevel1());
                    result.setReason("最近半年出现连续涨停");
                    result.setDetail(String.format("最大连续涨停天数: %d天, 出现日期: %tF",
                            limitUpInfo.maxConsecutiveDays, limitUpInfo.startDate));

                    results.add(result);
                }
            } catch (Exception e) {
                logger.error("分析股票 {} 时出错: {}", stockCode, e.getMessage());
            }
        }

        logger.info("共找到 {} 只符合连续涨停条件的股票", results.size());
        return results;
    }

    /**
     * 检查是否有连续涨停
     */
    private ConsecutiveLimitUpInfo checkConsecutiveLimitUp(List<CurrentStockData> stockDataList) {
        ConsecutiveLimitUpInfo info = new ConsecutiveLimitUpInfo();
        int currentConsecutiveDays = 0;
        Date currentStartDate = null;

        for (int i = 1; i < stockDataList.size(); i++) {
            CurrentStockData prevData = stockDataList.get(i - 1);
            CurrentStockData currentData = stockDataList.get(i);

            // 计算涨幅
            if (isLimitUp(prevData, currentData)) {
                if (currentConsecutiveDays == 0) {
                    currentStartDate = currentData.getTradeDate();
                }
                currentConsecutiveDays++;

                // 更新最大连续涨停天数
                if (currentConsecutiveDays > info.maxConsecutiveDays) {
                    info.maxConsecutiveDays = currentConsecutiveDays;
                    info.startDate = currentStartDate;
                    info.hasConsecutiveLimitUp = currentConsecutiveDays >= 2;
                }
            } else {
                currentConsecutiveDays = 0;
                currentStartDate = null;
            }
        }

        return info;
    }

    /**
     * 判断是否涨停
     */
    private boolean isLimitUp(CurrentStockData prevData, CurrentStockData currentData) {
        if (prevData.getClosePrice() == null || currentData.getClosePrice() == null) {
            return false;
        }

        BigDecimal prevClose = prevData.getClosePrice();
        BigDecimal currentClose = currentData.getClosePrice();

        // 计算涨幅 = (当前收盘价 - 前一日收盘价) / 前一日收盘价
        BigDecimal change = currentClose.subtract(prevClose)
                .divide(prevClose, 4, RoundingMode.HALF_UP);

        // 判断是否达到涨停阈值
        return change.compareTo(LIMIT_UP_THRESHOLD) >= 0;
    }

    @Override
    public List<StockSelectionResult> findHistoryTwoWavesStocks() {
        List<StockSelectionResult> results = new ArrayList<>();

        // 计算一年前的日期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        Date oneYearAgo = calendar.getTime();
        Date today = new Date();

        // 获取所有股票代码
        List<String> stockCodes = currentStockDataMapper.selectAllStockCodes();
        logger.info("开始分析 {} 只股票的历史二波情况", stockCodes.size());

        for (String stockCode : stockCodes) {
            try {
                // 查询该股票过去一年的交易数据
                List<CurrentStockData> stockDataList = currentStockDataMapper
                        .selectByStockCodeAndDateRange(stockCode, oneYearAgo, today);

                if (stockDataList == null || stockDataList.size() < 10) {
                    continue;
                }

                // 检查是否符合历史二波模式
                TwoWavesInfo twoWavesInfo = checkTwoWaves(stockDataList);

                if (twoWavesInfo.hasTwoWaves) {
                    CurrentStockData latestData = stockDataList.get(stockDataList.size() - 1);

                    StockSelectionResult result = new StockSelectionResult();
                    result.setStockCode(stockCode);
                    result.setStockName(latestData.getStockName());
                    result.setLatestTradeDate(latestData.getTradeDate());
                    result.setLatestClosePrice(latestData.getClosePrice());
                    result.setIndustryLevel1(latestData.getIndustryLevel1());
                    result.setReason("过去一年出现历史二波模式");
                    result.setDetail(String.format("第一波: %tF (连续%d天), 第二波: %tF (连续%d天), 回调幅度: %.2f%%",
                            twoWavesInfo.firstWaveDate, twoWavesInfo.firstWaveDays,
                            twoWavesInfo.secondWaveDate, twoWavesInfo.secondWaveDays,
                            twoWavesInfo.pullbackRatio.multiply(new BigDecimal("100"))));

                    results.add(result);
                }
            } catch (Exception e) {
                logger.error("分析股票 {} 时出错: {}", stockCode, e.getMessage());
            }
        }

        logger.info("共找到 {} 只符合历史二波条件的股票", results.size());
        return results;
    }

    /**
     * 检查是否符合历史二波模式
     * 标准：1. 出现第一波连续涨停 2. 涨到高点后回落调整（回调幅度>5%）3. 再次出现连续涨停
     */
    private TwoWavesInfo checkTwoWaves(List<CurrentStockData> stockDataList) {
        TwoWavesInfo info = new TwoWavesInfo();

        // 找出所有连续涨停的波段
        List<WaveSegment> waves = findAllConsecutiveLimitUpWaves(stockDataList);

        // 需要至少两波连续涨停
        if (waves.size() < 2) {
            return info;
        }

        // 检查任意两波之间是否存在有效的回调
        for (int i = 0; i < waves.size() - 1; i++) {
            WaveSegment firstWave = waves.get(i);

            for (int j = i + 1; j < waves.size(); j++) {
                WaveSegment secondWave = waves.get(j);

                // 检查两波之间是否有足够的调整期和回调幅度
                if (hasValidPullback(stockDataList, firstWave, secondWave)) {
                    info.hasTwoWaves = true;
                    info.firstWaveDate = firstWave.startDate;
                    info.firstWaveDays = firstWave.consecutiveDays;
                    info.secondWaveDate = secondWave.startDate;
                    info.secondWaveDays = secondWave.consecutiveDays;
                    info.pullbackRatio = calculatePullbackRatio(stockDataList, firstWave, secondWave);
                    return info;
                }
            }
        }

        return info;
    }

    /**
     * 找出所有连续涨停的波段
     */
    private List<WaveSegment> findAllConsecutiveLimitUpWaves(List<CurrentStockData> stockDataList) {
        List<WaveSegment> waves = new ArrayList<>();
        int consecutiveDays = 0;
        int startIndex = -1;

        for (int i = 1; i < stockDataList.size(); i++) {
            CurrentStockData prevData = stockDataList.get(i - 1);
            CurrentStockData currentData = stockDataList.get(i);

            if (isLimitUp(prevData, currentData)) {
                if (consecutiveDays == 0) {
                    startIndex = i;
                }
                consecutiveDays++;
            } else {
                if (consecutiveDays >= 2) {
                    WaveSegment wave = new WaveSegment();
                    wave.startIndex = startIndex;
                    wave.endIndex = i - 1;
                    wave.consecutiveDays = consecutiveDays;
                    wave.startDate = stockDataList.get(startIndex).getTradeDate();
                    wave.endDate = stockDataList.get(i - 1).getTradeDate();
                    wave.peakPrice = stockDataList.get(i - 1).getClosePrice();
                    waves.add(wave);
                }
                consecutiveDays = 0;
                startIndex = -1;
            }
        }

        // 处理最后一段
        if (consecutiveDays >= 2) {
            WaveSegment wave = new WaveSegment();
            wave.startIndex = startIndex;
            wave.endIndex = stockDataList.size() - 1;
            wave.consecutiveDays = consecutiveDays;
            wave.startDate = stockDataList.get(startIndex).getTradeDate();
            wave.endDate = stockDataList.get(stockDataList.size() - 1).getTradeDate();
            wave.peakPrice = stockDataList.get(stockDataList.size() - 1).getClosePrice();
            waves.add(wave);
        }

        return waves;
    }

    /**
     * 检查两波之间是否有有效的回调
     * 标准：1. 两波之间至少间隔5个交易日 2. 回调幅度至少5%
     */
    private boolean hasValidPullback(List<CurrentStockData> stockDataList,
                                      WaveSegment firstWave, WaveSegment secondWave) {
        // 间隔天数检查
        int gap = secondWave.startIndex - firstWave.endIndex;
        if (gap < 5) {
            return false;
        }

        // 找出第一波结束后到第二波开始前的最低点
        BigDecimal lowestPrice = null;
        for (int i = firstWave.endIndex + 1; i < secondWave.startIndex; i++) {
            BigDecimal closePrice = stockDataList.get(i).getClosePrice();
            if (closePrice != null) {
                if (lowestPrice == null || closePrice.compareTo(lowestPrice) < 0) {
                    lowestPrice = closePrice;
                }
            }
        }

        if (lowestPrice == null || firstWave.peakPrice == null) {
            return false;
        }

        // 计算回调幅度 = (高点价格 - 最低价格) / 高点价格
        BigDecimal pullbackRatio = firstWave.peakPrice.subtract(lowestPrice)
                .divide(firstWave.peakPrice, 4, RoundingMode.HALF_UP);

        // 回调幅度至少5%
        return pullbackRatio.compareTo(new BigDecimal("0.05")) >= 0;
    }

    /**
     * 计算回调幅度
     */
    private BigDecimal calculatePullbackRatio(List<CurrentStockData> stockDataList,
                                               WaveSegment firstWave, WaveSegment secondWave) {
        BigDecimal lowestPrice = null;
        for (int i = firstWave.endIndex + 1; i < secondWave.startIndex; i++) {
            BigDecimal closePrice = stockDataList.get(i).getClosePrice();
            if (closePrice != null) {
                if (lowestPrice == null || closePrice.compareTo(lowestPrice) < 0) {
                    lowestPrice = closePrice;
                }
            }
        }

        if (lowestPrice == null || firstWave.peakPrice == null) {
            return BigDecimal.ZERO;
        }

        return firstWave.peakPrice.subtract(lowestPrice)
                .divide(firstWave.peakPrice, 4, RoundingMode.HALF_UP);
    }

    /**
     * 连续涨停信息
     */
    private static class ConsecutiveLimitUpInfo {
        boolean hasConsecutiveLimitUp = false;
        int maxConsecutiveDays = 0;
        Date startDate = null;
    }

    /**
     * 二波信息
     */
    private static class TwoWavesInfo {
        boolean hasTwoWaves = false;
        Date firstWaveDate = null;
        int firstWaveDays = 0;
        Date secondWaveDate = null;
        int secondWaveDays = 0;
        BigDecimal pullbackRatio = BigDecimal.ZERO;
    }

    /**
     * 波段信息
     */
    private static class WaveSegment {
        int startIndex;
        int endIndex;
        int consecutiveDays;
        Date startDate;
        Date endDate;
        BigDecimal peakPrice;
    }
}
