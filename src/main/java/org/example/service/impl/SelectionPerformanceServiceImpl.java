package org.example.service.impl;

import org.example.entity.CurrentStockData;
import org.example.entity.SelectedStock;
import org.example.mapper.CurrentStockDataMapper;
import org.example.mapper.SelectedStockMapper;
import org.example.service.SelectionPerformanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 选股表现计算服务实现
 */
@Service
public class SelectionPerformanceServiceImpl implements SelectionPerformanceService {

    private static final Logger logger = LoggerFactory.getLogger(SelectionPerformanceServiceImpl.class);

    @Autowired
    private SelectedStockMapper selectedStockMapper;

    @Autowired
    private CurrentStockDataMapper currentStockDataMapper;

    @Override
    public boolean calculateAndUpdatePerformance(Long selectedStockId) {
        SelectedStock selectedStock = selectedStockMapper.findById(selectedStockId);
        if (selectedStock == null) {
            logger.warn("未找到ID为 {} 的选股记录", selectedStockId);
            return false;
        }

        try {
            // 1. 获取选股日收盘价
            CurrentStockData selectionDayData = currentStockDataMapper.selectByStockCodeAndDate(
                    selectedStock.getStockCode(), selectedStock.getSelectionDate());

            if (selectionDayData == null || selectionDayData.getClosePrice() == null) {
                logger.warn("未找到股票 {} 在选股日 {} 的数据",
                        selectedStock.getStockCode(), selectedStock.getSelectionDate());
                return false;
            }

            BigDecimal selectionClosePrice = selectionDayData.getClosePrice();
            selectedStock.setSelectionClosePrice(selectionClosePrice);

            // 2. 获取次日开盘价（买入价）
            CurrentStockData nextDayData = currentStockDataMapper.selectByStockCodeAndDateOffset(
                    selectedStock.getStockCode(), selectedStock.getSelectionDate(), 0);

            if (nextDayData == null || nextDayData.getOpenPrice() == null) {
                logger.warn("未找到股票 {} 选股次日的开盘价", selectedStock.getStockCode());
                return false;
            }

            BigDecimal buyPrice = nextDayData.getOpenPrice();
            selectedStock.setNextDayOpenPrice(buyPrice);

            // 3. 计算1天后涨跌幅（次日收盘价相对次日开盘价）
            if (nextDayData.getClosePrice() != null) {
                BigDecimal gain1day = calculateGain(buyPrice, nextDayData.getClosePrice());
                selectedStock.setGain1day(gain1day);
            }

            // 4. 计算3天后涨跌幅
            CurrentStockData day3Data = currentStockDataMapper.selectByStockCodeAndDateOffset(
                    selectedStock.getStockCode(), selectedStock.getSelectionDate(), 2);
            if (day3Data != null && day3Data.getClosePrice() != null) {
                BigDecimal gain3day = calculateGain(buyPrice, day3Data.getClosePrice());
                selectedStock.setGain3day(gain3day);
            }

            // 5. 计算7天后涨跌幅
            CurrentStockData day7Data = currentStockDataMapper.selectByStockCodeAndDateOffset(
                    selectedStock.getStockCode(), selectedStock.getSelectionDate(), 6);
            if (day7Data != null && day7Data.getClosePrice() != null) {
                BigDecimal gain7day = calculateGain(buyPrice, day7Data.getClosePrice());
                selectedStock.setGain7day(gain7day);
            }

            // 6. 更新数据库
            int updated = selectedStockMapper.updatePerformance(selectedStock);
            if (updated > 0) {
                logger.debug("更新选股记录 {} 的涨跌幅成功: 1天={}, 3天={}, 7天={}",
                        selectedStock.getStockCode(),
                        selectedStock.getGain1day(),
                        selectedStock.getGain3day(),
                        selectedStock.getGain7day());
            }
            return updated > 0;

        } catch (Exception e) {
            logger.error("计算选股记录 {} 的涨跌幅失败", selectedStockId, e);
            return false;
        }
    }

    @Override
    public int calculateAndUpdatePerformanceByDate(Date selectionDate) {
        List<SelectedStock> stocks = selectedStockMapper.findByStrategyAndDate(null, selectionDate);
        if (stocks == null || stocks.isEmpty()) {
            logger.info("选股日期 {} 没有选股记录", selectionDate);
            return 0;
        }

        int successCount = 0;
        for (SelectedStock stock : stocks) {
            if (calculateAndUpdatePerformance(stock.getId())) {
                successCount++;
            }
        }

        logger.info("选股日期 {} 的涨跌幅计算完成，成功更新 {}/{} 条记录",
                selectionDate, successCount, stocks.size());
        return successCount;
    }

    @Override
    public int calculateAndUpdatePerformanceByStrategyAndDate(String strategyCode, Date selectionDate) {
        List<SelectedStock> stocks = selectedStockMapper.findByStrategyAndDate(strategyCode, selectionDate);
        if (stocks == null || stocks.isEmpty()) {
            logger.info("策略 {} 在日期 {} 没有选股记录", strategyCode, selectionDate);
            return 0;
        }

        int successCount = 0;
        for (SelectedStock stock : stocks) {
            if (calculateAndUpdatePerformance(stock.getId())) {
                successCount++;
            }
        }

        logger.info("策略 {} 在日期 {} 的涨跌幅计算完成，成功更新 {}/{} 条记录",
                strategyCode, selectionDate, successCount, stocks.size());
        return successCount;
    }

    /**
     * 计算涨跌幅（百分比）
     *
     * @param buyPrice 买入价
     * @param sellPrice 卖出价
     * @return 涨跌幅（小数形式，如0.05表示5%）
     */
    private BigDecimal calculateGain(BigDecimal buyPrice, BigDecimal sellPrice) {
        if (buyPrice == null || buyPrice.compareTo(BigDecimal.ZERO) == 0 || sellPrice == null) {
            return null;
        }

        // 涨跌幅 = (卖出价 - 买入价) / 买入价
        BigDecimal gain = sellPrice.subtract(buyPrice)
                .divide(buyPrice, 6, RoundingMode.HALF_UP);

        return gain.setScale(4, RoundingMode.HALF_UP);
    }
}
