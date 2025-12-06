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
    private static final BigDecimal LIMIT_UP_THRESHOLD = new BigDecimal("0.098");

    @Override
    public List<StockSelectionResult> selectStocks(String strategyCode) {
        StockSelectionStrategy strategy = StockSelectionStrategy.fromCode(strategyCode);
        return selectStocks(strategy);
    }

    @Override
    public List<StockSelectionResult> selectStocks(String strategyCode, Date selectionDate) {
        StockSelectionStrategy strategy = StockSelectionStrategy.fromCode(strategyCode);
        switch (strategy) {
            case CONSECUTIVE_LIMIT_UP:
                return findConsecutiveLimitUpStocks();
            case UP_DOWN_UP:
                return findUpDownUpStocks();
            case HISTORY_TWO_WAVES:
                return findHistoryTwoWavesStocks();
            case MA_CONVERGENCE:
                return findMAConvergenceStocks(selectionDate);
            case BREAK_MA5_CLOSE_MA10:
                return findBreakMA5CloseMA10Stocks(selectionDate);
            case CONVERGING_TRIANGLE:
                return findConvergingTriangleStocks(selectionDate);
            case BOTTOM_VOLUME:
            case MA_BULLISH:
                throw new UnsupportedOperationException("策略 " + strategy.getDescription() + " 尚未实现");
            default:
                throw new IllegalArgumentException("未知的策略: " + strategy);
        }
    }

    @Override
    public List<StockSelectionResult> selectStocks(StockSelectionStrategy strategy) {
        switch (strategy) {
            case CONSECUTIVE_LIMIT_UP:
                return findConsecutiveLimitUpStocks();
            case UP_DOWN_UP:
                return findUpDownUpStocks();
            case HISTORY_TWO_WAVES:
                return findHistoryTwoWavesStocks();
            case MA_CONVERGENCE:
                return findMAConvergenceStocks();
            case BREAK_MA5_CLOSE_MA10:
                return findBreakMA5CloseMA10Stocks();
            case CONVERGING_TRIANGLE:
                return findConvergingTriangleStocks();
            case BOTTOM_VOLUME:
            case MA_BULLISH:
                throw new UnsupportedOperationException("策略 " + strategy.getDescription() + " 尚未实现");
            default:
                throw new IllegalArgumentException("未知的策略: " + strategy);
        }
    }

    @Override
    public List<StockSelectionResult> findUpDownUpStocks(){
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
                // 检查是否有涨停-跌停-涨停
                ConsecutiveLimitUpInfo limitUpInfo = checkUpDownUp(stockDataList);

                if (limitUpInfo.hasConsecutiveLimitUp) {
                    CurrentStockData latestData = stockDataList.get(stockDataList.size() - 1);

                    StockSelectionResult result = new StockSelectionResult();
                    result.setStockCode(stockCode);
                    result.setStockName(latestData.getStockName());
                    result.setLatestTradeDate(latestData.getTradeDate());
                    result.setLatestClosePrice(latestData.getClosePrice());
                    result.setIndustryLevel1(latestData.getIndustryLevel1());
                    result.setReason("最近半年出现涨停-跌停-涨停"+String.format("出现日期: %tF",
                            limitUpInfo.startDate));
                    result.setDetail(String.format("出现日期: %tF",
                             limitUpInfo.startDate));

                    results.add(result);
                }
            } catch (Exception e) {
                logger.error("分析股票 {} 时出错: {}", stockCode, e.getMessage());
            }
        }

        logger.info("共找到 {} 只符合连续涨停条件的股票", results.size());
        return results;
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
     *涨停-跌停-涨停
     */
    private ConsecutiveLimitUpInfo checkUpDownUp(List<CurrentStockData> stockDataList) {
        ConsecutiveLimitUpInfo info = new ConsecutiveLimitUpInfo();
        Date currentStartDate = null;
        for(int i=1;i< stockDataList.size()-3;i++){
            CurrentStockData prevData = stockDataList.get(i - 1);
            CurrentStockData currentData = stockDataList.get(i);
            CurrentStockData nextData = stockDataList.get(i+1);
            CurrentStockData nextTwoData = stockDataList.get(i+2);
            // 第一天涨停
            if (isLimitUp(prevData, currentData)) {
                //第二天跌停
                if(isLimitDown(currentData,nextData)){
                    //第三天涨停
                    if(isLimitUp(nextData, nextTwoData)){
                        currentStartDate = currentData.getTradeDate();
                        info.startDate = currentStartDate;
                        info.hasConsecutiveLimitUp = true;
                        return info;
                    }
                }
            }
        }
        return info;
    }


    /**
     * 检查是否有连续涨停
     */
    private ConsecutiveLimitUpInfo checkConsecutiveLimitUp(List<CurrentStockData> stockDataList) {
        return checkConsecutiveLimitUp(stockDataList, 1, stockDataList.size() - 1);
    }



    /**
     * 检查指定区间内是否有连续涨停
     */
    private ConsecutiveLimitUpInfo checkConsecutiveLimitUp(List<CurrentStockData> stockDataList,
                                                            int startIndex, int endIndex) {
        ConsecutiveLimitUpInfo info = new ConsecutiveLimitUpInfo();
        int currentConsecutiveDays = 0;
        Date currentStartDate = null;

        for (int i = Math.max(1, startIndex); i <= Math.min(endIndex, stockDataList.size() - 1); i++) {
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

    /**
     * 判断是否跌停
     */
    private boolean isLimitDown(CurrentStockData prevData, CurrentStockData currentData) {
        if (prevData.getClosePrice() == null || currentData.getClosePrice() == null) {
            return false;
        }

        BigDecimal prevClose = prevData.getClosePrice();
        BigDecimal currentClose = currentData.getClosePrice();
        // 计算跌幅 = （前一日收盘价-当前收盘价）/前一日收盘价
        // 计算涨幅 = (当前收盘价 - 前一日收盘价) / 前一日收盘价
        BigDecimal change = prevClose.subtract(currentClose)
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

    /**
     * 均线粘合策略（使用当前日期）
     * 创业板：过去90天最大涨幅>15%，五日线拐头向上，五日线接近十日线和二十日线，股价低于半年最高价20%以上
     * 主板：过去60天有连续涨停，五日线拐头向上，五日线接近十日线和二十日线，股价低于半年最高价20%以上
     */
    @Override
    public List<StockSelectionResult> findMAConvergenceStocks() {
        return findMAConvergenceStocks(new Date());
    }

    /**
     * 均线粘合策略（指定日期）
     * 基于指定日期收盘进行选股
     */
    @Override
    public List<StockSelectionResult> findMAConvergenceStocks(Date selectionDate) {
        List<StockSelectionResult> results = new ArrayList<>();

        // 获取1年的数据用于计算（从选股日期向前推1年）
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectionDate);
        calendar.add(Calendar.YEAR, -1);
        Date oneYearAgo = calendar.getTime();

        List<String> stockCodes = currentStockDataMapper.selectAllStockCodes();

        logger.info("开始均线粘合策略选股，选股日期: {}, 共 {} 只股票", selectionDate, stockCodes.size());

        for (String stockCode : stockCodes) {
            try {
                List<CurrentStockData> stockDataList = currentStockDataMapper
                        .selectByStockCodeAndDateRange(stockCode, oneYearAgo, selectionDate);

                if (stockDataList == null || stockDataList.size() < 180) {
                    continue; // 数据不足
                }

                // 排除ST股票
                if (isSTStock(stockCode, stockDataList)) {
                    continue;
                }

                // 判断是创业板还是主板
                boolean isChYB = isChYBStock(stockCode);

                MAConvergenceInfo info;
                if (isChYB) {
                    info = checkChYBMAConvergence(stockDataList);
                } else {
                    info = checkMainBoardMAConvergence(stockDataList);
                }

                if (info.matches) {
                    StockSelectionResult result = new StockSelectionResult();
                    result.setStockCode(stockCode);
                    result.setStockName(stockDataList.get(stockDataList.size() - 1).getStockName());
                    result.setReason(info.reason);
                    results.add(result);

                    logger.info("发现均线粘合股票: {} - {}", stockCode, info.reason);
                }

            } catch (Exception e) {
                logger.error("处理股票 {} 时出错: {}", stockCode, e.getMessage(), e);
            }
        }

        logger.info("均线粘合策略选股完成，共找到 {} 只股票", results.size());
        return results;
    }

    /**
     * 检查创业板均线粘合条件
     */
    private MAConvergenceInfo checkChYBMAConvergence(List<CurrentStockData> dataList) {
        MAConvergenceInfo info = new MAConvergenceInfo();
        int size = dataList.size();

        // 需要至少180天数据
        if (size < 180) {
            return info;
        }

        CurrentStockData latestData = dataList.get(size - 1);

        // 检查是否有均线数据
        if (latestData.getMa5() == null || latestData.getMa10() == null || latestData.getMa20() == null) {
            return info;
        }

        // 1. 五日线拐头向上
        if (!isMa5TurningUp(dataList, size - 1)) {
            return info;
        }

        // 2. 五日线接近十日线（差距在3%以内）
        if (!isMaClose(latestData.getMa5(), latestData.getMa10(), 3.0)) {
            return info;
        }

        // 3. 五日线接近二十日线（差距在5%以内）
        if (!isMaClose(latestData.getMa5(), latestData.getMa20(), 5.0)) {
            return info;
        }

        // 4. 二十日线不能是下降趋势
        if (!isMa20NotDowntrend(dataList, size - 1)) {
            return info;
        }

        // 5. 过去90天内单日最大涨幅超过15%
        int daysToCheck = Math.min(90, size - 1);
        BigDecimal maxSingleDayGain = getMaxSingleDayGain(dataList, size - daysToCheck, size - 1);
        if (maxSingleDayGain.compareTo(new BigDecimal("0.15")) <= 0) {
            return info;
        }

        // 6. 当前股价低于半年内最高价10%以上
        int halfYearDays = Math.min(120, size);
        BigDecimal halfYearHighest = getHighestPrice(dataList, size - halfYearDays, size - 1);
        BigDecimal currentPrice = latestData.getClosePrice();

        if (halfYearHighest != null && currentPrice != null) {
            BigDecimal dropRatio = halfYearHighest.subtract(currentPrice)
                    .divide(halfYearHighest, 4, RoundingMode.HALF_UP);

            if (dropRatio.compareTo(new BigDecimal("0.25")) <= 0) {
                return info;
            }

            info.matches = true;
            info.reason = String.format("创业板均线粘合: MA5拐头向上,均线粘合,90天最大涨幅%.2f%%,距半年高点回落%.2f%%",
                    maxSingleDayGain.multiply(new BigDecimal("100")).doubleValue(),
                    dropRatio.multiply(new BigDecimal("100")).doubleValue());
        }

        return info;
    }

    /**
     * 检查主板均线粘合条件
     */
    private MAConvergenceInfo checkMainBoardMAConvergence(List<CurrentStockData> dataList) {
        MAConvergenceInfo info = new MAConvergenceInfo();
        int size = dataList.size();

        if (size < 180) {
            return info;
        }

        CurrentStockData latestData = dataList.get(size - 1);

        // 检查是否有均线数据
        if (latestData.getMa5() == null || latestData.getMa10() == null || latestData.getMa20() == null) {
            return info;
        }

        // 1. 五日线拐头向上
        if (!isMa5TurningUp(dataList, size - 1)) {
            return info;
        }

        // 2. 五日线接近十日线（差距在3%以内）
        if (!isMaClose(latestData.getMa5(), latestData.getMa10(), 3.0)) {
            return info;
        }

        // 3. 五日线接近二十日线（差距在5%以内）
        if (!isMaClose(latestData.getMa5(), latestData.getMa20(), 5.0)) {
            return info;
        }

        // 4. 二十日线不能是下降趋势
        if (!isMa20NotDowntrend(dataList, size - 1)) {
            return info;
        }

        // 5. 过去60天内有连续涨停
        int daysToCheck = Math.min(60, size - 1);
        ConsecutiveLimitUpInfo limitUpInfo = checkConsecutiveLimitUp(dataList, size - daysToCheck, size - 1);
        if (!limitUpInfo.hasConsecutiveLimitUp) {
            return info;
        }

        // 6. 当前股价低于半年内最高价20%以上
        int halfYearDays = Math.min(120, size);
        BigDecimal halfYearHighest = getHighestPrice(dataList, size - halfYearDays, size - 1);
        BigDecimal currentPrice = latestData.getClosePrice();

        if (halfYearHighest != null && currentPrice != null) {
            BigDecimal dropRatio = halfYearHighest.subtract(currentPrice)
                    .divide(halfYearHighest, 4, RoundingMode.HALF_UP);

            if (dropRatio.compareTo(new BigDecimal("0.20")) <= 0) {
                return info;
            }

            info.matches = true;
            info.reason = String.format("主板均线粘合: MA5拐头向上,均线粘合,60天内有%d连板,距半年高点回落%.2f%%",
                    limitUpInfo.maxConsecutiveDays,
                    dropRatio.multiply(new BigDecimal("100")).doubleValue());
        }

        return info;
    }

    /**
     * 判断五日线是否拐头向上
     * 逻辑：当前MA5 > 前一日MA5 且 前一日MA5 > 前两日MA5
     */
    private boolean isMa5TurningUp(List<CurrentStockData> dataList, int currentIndex) {
        if (currentIndex < 2) {
            return false;
        }

        BigDecimal currentMa5 = dataList.get(currentIndex).getMa5();
        BigDecimal prevMa5 = dataList.get(currentIndex - 1).getMa5();
        BigDecimal prev2Ma5 = dataList.get(currentIndex - 2).getMa5();

        if (currentMa5 == null || prevMa5 == null || prev2Ma5 == null) {
            return false;
        }

        return currentMa5.compareTo(prevMa5) > 0 && prevMa5.compareTo(prev2Ma5) > 0;
    }

    /**
     * 判断二十日线是否不是下降趋势
     * 逻辑：当前MA20 >= 5日前MA20（允许持平或向上）
     */
    private boolean isMa20NotDowntrend(List<CurrentStockData> dataList, int currentIndex) {
        if (currentIndex < 5) {
            return false;
        }

        BigDecimal currentMa20 = dataList.get(currentIndex).getMa20();
        BigDecimal prev5Ma20 = dataList.get(currentIndex - 5).getMa20();

        if (currentMa20 == null || prev5Ma20 == null) {
            return false;
        }

        // 当前MA20大于等于5天前的MA20，说明不是下降趋势
        return currentMa20.compareTo(prev5Ma20) >= 0;
    }

    /**
     * 判断两条均线是否接近
     * @param ma1 均线1
     * @param ma2 均线2
     * @param thresholdPercent 阈值百分比（例如3.0表示3%）
     */
    private boolean isMaClose(BigDecimal ma1, BigDecimal ma2, double thresholdPercent) {
        if (ma1 == null || ma2 == null || ma2.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal diff = ma1.subtract(ma2).abs();
        BigDecimal ratio = diff.divide(ma2, 4, RoundingMode.HALF_UP);

        return ratio.compareTo(new BigDecimal(thresholdPercent / 100)) <= 0;
    }

    /**
     * 获取指定区间内的单日最大涨幅
     */
    private BigDecimal getMaxSingleDayGain(List<CurrentStockData> dataList, int startIndex, int endIndex) {
        BigDecimal maxGain = BigDecimal.ZERO;

        for (int i = startIndex + 1; i <= endIndex; i++) {
            BigDecimal prevClose = dataList.get(i - 1).getClosePrice();
            BigDecimal currentClose = dataList.get(i).getClosePrice();

            if (prevClose != null && currentClose != null && prevClose.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal gain = currentClose.subtract(prevClose)
                        .divide(prevClose, 4, RoundingMode.HALF_UP);

                if (gain.compareTo(maxGain) > 0) {
                    maxGain = gain;
                }
            }
        }

        return maxGain;
    }

    /**
     * 获取指定区间内的最高价
     */
    private BigDecimal getHighestPrice(List<CurrentStockData> dataList, int startIndex, int endIndex) {
        BigDecimal highest = null;

        for (int i = startIndex; i <= endIndex; i++) {
            BigDecimal highPrice = dataList.get(i).getHighPrice();
            if (highPrice != null) {
                if (highest == null || highPrice.compareTo(highest) > 0) {
                    highest = highPrice;
                }
            }
        }

        return highest;
    }

    /**
     * 判断是否为ST股票
     */
    private boolean isSTStock(String stockCode, List<CurrentStockData> dataList) {
        if (dataList.isEmpty()) {
            return false;
        }

        String stockName = dataList.get(dataList.size() - 1).getStockName();
        return stockName != null && (stockName.contains("ST") || stockName.contains("*ST"));
    }

    /**
     * 判断是否为创业板股票（300开头）
     */
    private boolean isChYBStock(String stockCode) {
        return stockCode != null && stockCode.startsWith("300");
    }

    /**
     * 均线粘合信息
     */
    private static class MAConvergenceInfo {
        boolean matches = false;
        String reason = "";
    }

    /**
     * 破五收十策略（使用当前日期）
     * 条件：1. 最近20天内有过连续涨停 2. 之后股价跌破五日线 3. 最近一个交易日收红 4. 收盘价在十日线之上
     */
    @Override
    public List<StockSelectionResult> findBreakMA5CloseMA10Stocks() {
        return findBreakMA5CloseMA10Stocks(new Date());
    }

    /**
     * 破五收十策略（指定日期）
     * 基于指定日期收盘进行选股
     */
    @Override
    public List<StockSelectionResult> findBreakMA5CloseMA10Stocks(Date selectionDate) {
        List<StockSelectionResult> results = new ArrayList<>();

        // 获取60天的数据用于计算（从选股日期向前推60天，确保有足够数据）
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectionDate);
        calendar.add(Calendar.DAY_OF_YEAR, -60);
        Date sixtyDaysAgo = calendar.getTime();

        List<String> stockCodes = currentStockDataMapper.selectAllStockCodes();

        logger.info("开始破五收十策略选股，选股日期: {}, 共 {} 只股票", selectionDate, stockCodes.size());

        for (String stockCode : stockCodes) {
            try {
                List<CurrentStockData> stockDataList = currentStockDataMapper
                        .selectByStockCodeAndDateRange(stockCode, sixtyDaysAgo, selectionDate);

                if (stockDataList == null || stockDataList.size() < 30) {
                    continue; // 数据不足
                }

                // 排除ST股票
                if (isSTStock(stockCode, stockDataList)) {
                    continue;
                }

                // 检查是否符合破五收十条件
                BreakMA5CloseMA10Info info = checkBreakMA5CloseMA10(stockDataList);

                if (info.matches) {
                    CurrentStockData latestData = stockDataList.get(stockDataList.size() - 1);

                    StockSelectionResult result = new StockSelectionResult();
                    result.setStockCode(stockCode);
                    result.setStockName(latestData.getStockName());
                    result.setLatestTradeDate(latestData.getTradeDate());
                    result.setLatestClosePrice(latestData.getClosePrice());
                    result.setIndustryLevel1(latestData.getIndustryLevel1());
                    result.setReason(info.reason);
                    result.setDetail(info.detail);
                    results.add(result);

                    logger.info("发现破五收十股票: {} - {}", stockCode, info.reason);
                }

            } catch (Exception e) {
                logger.error("处理股票 {} 时出错: {}", stockCode, e.getMessage(), e);
            }
        }

        logger.info("破五收十策略选股完成，共找到 {} 只股票", results.size());
        return results;
    }

    /**
     * 检查是否符合破五收十条件
     * 1. 最近20天内有过连续涨停（至少2天）
     * 2. 涨停之后股价跌破五日线
     * 3. 最近一个交易日收红（收盘价>开盘价）
     * 4. 最近一个交易日收盘价在十日线之上
     */
    private BreakMA5CloseMA10Info checkBreakMA5CloseMA10(List<CurrentStockData> dataList) {
        BreakMA5CloseMA10Info info = new BreakMA5CloseMA10Info();
        int size = dataList.size();

        if (size < 30) {
            return info;
        }

        CurrentStockData latestData = dataList.get(size - 1);

        // 检查是否有均线数据
        if (latestData.getMa5() == null || latestData.getMa10() == null) {
            return info;
        }

        // 条件4：最近一个交易日收盘价在十日线之上
        if (latestData.getClosePrice().compareTo(latestData.getMa10()) <= 0) {
            return info;
        }

        // 条件3：最近一个交易日收红（收盘价 > 开盘价）
        if (latestData.getOpenPrice() == null ||
            latestData.getClosePrice().compareTo(latestData.getOpenPrice()) <= 0) {
            return info;
        }

        // 条件1：最近20天内有过连续涨停
        int lookbackDays = Math.min(20, size - 1);
        ConsecutiveLimitUpInfo limitUpInfo = checkConsecutiveLimitUp(dataList, size - lookbackDays, size - 1);

        if (!limitUpInfo.hasConsecutiveLimitUp) {
            return info;
        }

        // 条件2：涨停之后股价跌破五日线
        // 从涨停结束后开始查找，看是否有跌破五日线的情况
        boolean hasBrokenMA5 = false;
        int limitUpEndIndex = -1;

        // 找到最近一次涨停波段的结束位置
        for (int i = size - lookbackDays; i < size - 1; i++) {
            if (isLimitUp(dataList.get(i - 1), dataList.get(i))) {
                limitUpEndIndex = i;
            } else if (limitUpEndIndex > 0) {
                // 涨停结束后的第一个非涨停日
                break;
            }
        }

        // 检查涨停结束后是否有跌破五日线
        if (limitUpEndIndex > 0 && limitUpEndIndex < size - 1) {
            for (int i = limitUpEndIndex + 1; i < size; i++) {
                CurrentStockData data = dataList.get(i);
                if (data.getMa5() != null && data.getClosePrice() != null) {
                    // 股价跌破五日线（收盘价 < 五日线）
                    if (data.getClosePrice().compareTo(data.getMa5()) < 0) {
                        hasBrokenMA5 = true;
                        break;
                    }
                }
            }
        }

        if (!hasBrokenMA5) {
            return info;
        }

        // 所有条件都满足
        info.matches = true;
        info.reason = String.format("破五收十: 近20天有%d连板，后跌破MA5，今日收红且站上MA10",
                limitUpInfo.maxConsecutiveDays);
        info.detail = String.format("涨停日期: %tF, 连续%d天涨停; 今日收盘: %.2f, MA5: %.2f, MA10: %.2f",
                limitUpInfo.startDate,
                limitUpInfo.maxConsecutiveDays,
                latestData.getClosePrice().doubleValue(),
                latestData.getMa5().doubleValue(),
                latestData.getMa10().doubleValue());

        return info;
    }

    /**
     * 破五收十信息
     */
    private static class BreakMA5CloseMA10Info {
        boolean matches = false;
        String reason = "";
        String detail = "";
    }

    /**
     * 收敛三角形策略（使用当前日期）
     * 条件：一段时间内（30-60天）高点不断降低，低点不断上升，二者逐渐接近
     */
    @Override
    public List<StockSelectionResult> findConvergingTriangleStocks() {
        return findConvergingTriangleStocks(new Date());
    }

    /**
     * 收敛三角形策略（指定日期）
     * 基于指定日期收盘进行选股
     */
    @Override
    public List<StockSelectionResult> findConvergingTriangleStocks(Date selectionDate) {
        List<StockSelectionResult> results = new ArrayList<>();

        // 获取90天的数据用于计算（从选股日期向前推90天）
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectionDate);
        calendar.add(Calendar.DAY_OF_YEAR, -90);
        Date ninetyDaysAgo = calendar.getTime();

        List<String> stockCodes = currentStockDataMapper.selectAllStockCodes();

        logger.info("开始收敛三角形策略选股，选股日期: {}, 共 {} 只股票", selectionDate, stockCodes.size());

        for (String stockCode : stockCodes) {
            try {
                List<CurrentStockData> stockDataList = currentStockDataMapper
                        .selectByStockCodeAndDateRange(stockCode, ninetyDaysAgo, selectionDate);

                if (stockDataList == null || stockDataList.size() < 30) {
                    continue; // 数据不足
                }

                // 排除ST股票
                if (isSTStock(stockCode, stockDataList)) {
                    continue;
                }

                // 检查是否符合收敛三角形形态
                ConvergingTriangleInfo info = checkConvergingTriangle(stockDataList);

                if (info.matches) {
                    CurrentStockData latestData = stockDataList.get(stockDataList.size() - 1);

                    StockSelectionResult result = new StockSelectionResult();
                    result.setStockCode(stockCode);
                    result.setStockName(latestData.getStockName());
                    result.setLatestTradeDate(latestData.getTradeDate());
                    result.setLatestClosePrice(latestData.getClosePrice());
                    result.setIndustryLevel1(latestData.getIndustryLevel1());
                    result.setReason(info.reason);
                    result.setDetail(info.detail);
                    results.add(result);

                    logger.info("发现收敛三角形股票: {} - {}", stockCode, info.reason);
                }

            } catch (Exception e) {
                logger.error("处理股票 {} 时出错: {}", stockCode, e.getMessage(), e);
            }
        }

        logger.info("收敛三角形策略选股完成，共找到 {} 只股票", results.size());
        return results;
    }

    /**
     * 检查是否符合收敛三角形形态
     *
     * 收敛三角形特征：
     * 1. 时间周期：30-60个交易日
     * 2. 高点递减：至少3个高点，每个高点比前一个低
     * 3. 低点递增：至少3个低点，每个低点比前一个高
     * 4. 收敛趋势：高点和低点之间的差距逐渐缩小
     * 5. 突破迹象：最近股价接近收敛点（高低点差距 < 初始差距的30%）
     */
    private ConvergingTriangleInfo checkConvergingTriangle(List<CurrentStockData> dataList) {
        ConvergingTriangleInfo info = new ConvergingTriangleInfo();
        int size = dataList.size();

        String stockCode = dataList.isEmpty() ? "UNKNOWN" : dataList.get(0).getStockCode();
        String stockName = dataList.isEmpty() ? "UNKNOWN" : dataList.get(0).getStockName();

        // 至少需要30个交易日
        if (size < 30) {
            logger.debug("[{}] 数据不足: 仅{}天数据", stockCode, size);
            return info;
        }

        // 分析周期：取最近30-60天的数据
        int analysisPeriod = Math.min(90, size);
        int startIndex = size - analysisPeriod;

        // 找出高点和低点
        List<PricePoint> highPoints = findLocalHighPoints(dataList, startIndex, size - 1);
        List<PricePoint> lowPoints = findLocalLowPoints(dataList, startIndex, size - 1);

        logger.debug("[{}-{}] 分析周期{}天, 找到{}个高点, {}个低点",
                stockCode, stockName, analysisPeriod, highPoints.size(), lowPoints.size());

        // 至少需要3个高点和3个低点
        if (highPoints.size() < 3 || lowPoints.size() < 3) {
            logger.debug("[{}] 高低点数量不足: 高点{}, 低点{}", stockCode, highPoints.size(), lowPoints.size());
            return info;
        }

        // 打印找到的高点
        StringBuilder highPointsStr = new StringBuilder();
        for (PricePoint p : highPoints) {
            highPointsStr.append(String.format("%tF:%.2f ", p.date, p.price));
        }
        logger.debug("[{}] 高点: {}", stockCode, highPointsStr);

        // 打印找到的低点
        StringBuilder lowPointsStr = new StringBuilder();
        for (PricePoint p : lowPoints) {
            lowPointsStr.append(String.format("%tF:%.2f ", p.date, p.price));
        }
        logger.debug("[{}] 低点: {}", stockCode, lowPointsStr);

        // 检查高点是否递减
        boolean highsDecreasing = areHighsDecreasing(highPoints);
        logger.debug("[{}] 高点递减检查: {}", stockCode, highsDecreasing);
        if (!highsDecreasing) {
            return info;
        }

        // 检查低点是否递增
        boolean lowsIncreasing = areLowsIncreasing(lowPoints);
        logger.debug("[{}] 低点递增检查: {}", stockCode, lowsIncreasing);
        if (!lowsIncreasing) {
            return info;
        }

        // 检查是否收敛（高低点差距逐渐缩小）
        boolean isConverging = checkConvergence(highPoints, lowPoints);
        logger.debug("[{}] 收敛检查: {}", stockCode, isConverging);
        if (!isConverging) {
            return info;
        }

        // 计算收敛程度
        BigDecimal initialGap = highPoints.get(0).price.subtract(lowPoints.get(0).price);
        BigDecimal currentGap = highPoints.get(highPoints.size() - 1).price
                .subtract(lowPoints.get(lowPoints.size() - 1).price);

        BigDecimal convergenceRatio = BigDecimal.ZERO;
        if (initialGap.compareTo(BigDecimal.ZERO) > 0) {
            convergenceRatio = currentGap.divide(initialGap, 4, RoundingMode.HALF_UP);
        }

        logger.debug("[{}] 收敛度: 初始差距={}, 当前差距={}, 收敛比率={}",
                stockCode, initialGap, currentGap, convergenceRatio);

        // 要求当前差距 < 初始差距的50%，说明有明显收敛趋势
        // 同时要求当前差距不能太大（绝对值），避免选出波动太大的股票
        BigDecimal maxAbsoluteGap = new BigDecimal("1.5"); // 最大绝对差距1.5元

        if (convergenceRatio.compareTo(new BigDecimal("0.50")) > 0) {
            logger.debug("[{}] 收敛度不足: {}% (需要<=50%)",
                    stockCode, convergenceRatio.multiply(new BigDecimal("100")));
            return info;
        }

        if (currentGap.compareTo(maxAbsoluteGap) > 0) {
            logger.debug("[{}] 当前差距过大: {} (需要<={} )",
                    stockCode, currentGap, maxAbsoluteGap);
            return info;
        }

        // 所有条件都满足 - 打印完整的股票数据
        info.matches = true;
        info.stockCode = stockCode;
        info.stockName = stockName;
        info.reason = String.format("收敛三角形: %d个高点递减,%d个低点递增,收敛度%.0f%%",
                highPoints.size(), lowPoints.size(),
                (BigDecimal.ONE.subtract(convergenceRatio)).multiply(new BigDecimal("100")).doubleValue());

        CurrentStockData latestData = dataList.get(size - 1);
        info.detail = String.format("分析周期%d天; 初始差距%.2f,当前差距%.2f; 最新价%.2f",
                analysisPeriod,
                initialGap.doubleValue(),
                currentGap.doubleValue(),
                latestData.getClosePrice().doubleValue());

        // 打印完整的股票数据供调试
        logger.info("========== 发现收敛三角形: {}-{} ==========", stockCode, stockName);
        logger.info("分析区间: {} 到 {}",
                dataList.get(startIndex).getTradeDate(),
                dataList.get(size - 1).getTradeDate());
        logger.info("完整数据序列 (日期,开盘,最高,最低,收盘):");
        for (int i = startIndex; i <= size - 1; i++) {
            CurrentStockData data = dataList.get(i);
            logger.info("  {}: O={}, H={}, L={}, C={}",
                    String.format("%tF", data.getTradeDate()),
                    data.getOpenPrice(),
                    data.getHighPrice(),
                    data.getLowPrice(),
                    data.getClosePrice());
        }
        logger.info("高点序列: {}", highPointsStr);
        logger.info("低点序列: {}", lowPointsStr);
        logger.info("======================================================");

        return info;
    }

    /**
     * 找出局部高点
     * 局部高点定义：某个交易日的最高价高于前后各2个交易日的最高价
     */
    private List<PricePoint> findLocalHighPoints(List<CurrentStockData> dataList, int startIndex, int endIndex) {
        List<PricePoint> highPoints = new ArrayList<>();
        int windowSize = 3; // 前后各看3个交易日

        for (int i = startIndex + windowSize; i <= endIndex - windowSize; i++) {
            BigDecimal currentHigh = dataList.get(i).getHighPrice();
            if (currentHigh == null) {
                continue;
            }

            boolean isLocalHigh = true;

            // 检查前后窗口内的最高价
            for (int j = i - windowSize; j <= i + windowSize; j++) {
                if (j == i) continue;

                BigDecimal compareHigh = dataList.get(j).getHighPrice();
                if (compareHigh != null && compareHigh.compareTo(currentHigh) >= 0) {
                    isLocalHigh = false;
                    break;
                }
            }

            if (isLocalHigh) {
                PricePoint point = new PricePoint();
                point.index = i;
                point.date = dataList.get(i).getTradeDate();
                point.price = currentHigh;
                highPoints.add(point);
            }
        }

        return highPoints;
    }

    /**
     * 找出局部低点
     * 局部低点定义：某个交易日的最低价低于前后各2个交易日的最低价
     */
    private List<PricePoint> findLocalLowPoints(List<CurrentStockData> dataList, int startIndex, int endIndex) {
        List<PricePoint> lowPoints = new ArrayList<>();
        int windowSize = 2; // 前后各看2个交易日

        for (int i = startIndex + windowSize; i <= endIndex - windowSize; i++) {
            BigDecimal currentLow = dataList.get(i).getLowPrice();
            if (currentLow == null) {
                continue;
            }

            boolean isLocalLow = true;

            // 检查前后窗口内的最低价
            for (int j = i - windowSize; j <= i + windowSize; j++) {
                if (j == i) continue;

                BigDecimal compareLow = dataList.get(j).getLowPrice();
                if (compareLow != null && compareLow.compareTo(currentLow) <= 0) {
                    isLocalLow = false;
                    break;
                }
            }

            if (isLocalLow) {
                PricePoint point = new PricePoint();
                point.index = i;
                point.date = dataList.get(i).getTradeDate();
                point.price = currentLow;
                lowPoints.add(point);
            }
        }

        return lowPoints;
    }

    /**
     * 检查高点是否递减
     * 要求至少60%的相邻高点对满足递减关系
     * 并且最后一个高点要低于第一个高点
     */
    private boolean areHighsDecreasing(List<PricePoint> highPoints) {
        if (highPoints.size() < 2) {
            return false;
        }

        int decreasingCount = 0;
        int totalPairs = highPoints.size() - 1;

        for (int i = 1; i < highPoints.size(); i++) {
            if (highPoints.get(i).price.compareTo(highPoints.get(i - 1).price) < 0) {
                decreasingCount++;
            }
        }

        // 至少60%的相邻高点递减，并且整体下降（最后一个 < 第一个）
        boolean pairwiseDecreasing = decreasingCount >= totalPairs * 0.6;
        boolean overallDecreasing = highPoints.get(highPoints.size() - 1).price
                .compareTo(highPoints.get(0).price) < 0;

        return pairwiseDecreasing && overallDecreasing;
    }

    /**
     * 检查低点是否递增
     * 要求至少60%的相邻低点对满足递增关系
     * 并且最后一个低点要高于第一个低点
     */
    private boolean areLowsIncreasing(List<PricePoint> lowPoints) {
        if (lowPoints.size() < 2) {
            return false;
        }

        int increasingCount = 0;
        int totalPairs = lowPoints.size() - 1;

        for (int i = 1; i < lowPoints.size(); i++) {
            if (lowPoints.get(i).price.compareTo(lowPoints.get(i - 1).price) > 0) {
                increasingCount++;
            }
        }

        // 至少60%的相邻低点递增，并且整体上升（最后一个 > 第一个）
        boolean pairwiseIncreasing = increasingCount >= totalPairs * 0.6;
        boolean overallIncreasing = lowPoints.get(lowPoints.size() - 1).price
                .compareTo(lowPoints.get(0).price) > 0;

        return pairwiseIncreasing && overallIncreasing;
    }

    /**
     * 检查是否收敛（高低点差距逐渐缩小）
     * 比较最近的高低点差距与最早的高低点差距
     */
    private boolean checkConvergence(List<PricePoint> highPoints, List<PricePoint> lowPoints) {
        if (highPoints.isEmpty() || lowPoints.isEmpty()) {
            return false;
        }

        // 找到时间上最早和最晚的高低点对
        BigDecimal initialGap = highPoints.get(0).price.subtract(lowPoints.get(0).price);
        BigDecimal currentGap = highPoints.get(highPoints.size() - 1).price
                .subtract(lowPoints.get(lowPoints.size() - 1).price);

        // 当前差距应该小于初始差距
        return currentGap.compareTo(initialGap) < 0;
    }

    /**
     * 价格点信息
     */
    private static class PricePoint {
        int index;          // 在数据列表中的索引
        Date date;          // 交易日期
        BigDecimal price;   // 价格
    }

    /**
     * 收敛三角形信息
     */
    private static class ConvergingTriangleInfo {
        boolean matches = false;
        String stockCode = "";
        String stockName = "";
        String reason = "";
        String detail = "";
    }
}
