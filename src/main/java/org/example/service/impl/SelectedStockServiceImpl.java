package org.example.service.impl;

import org.example.dto.StockSelectionResult;
import org.example.entity.SelectedStock;
import org.example.mapper.SelectedStockMapper;
import org.example.service.SelectedStockService;
import org.example.service.StockSelectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 已选股票服务实现
 */
@Service
public class SelectedStockServiceImpl implements SelectedStockService {

    private static final Logger logger = LoggerFactory.getLogger(SelectedStockServiceImpl.class);

    @Autowired
    private SelectedStockMapper selectedStockMapper;

    @Autowired
    private StockSelectionService stockSelectionService;

    @Autowired
    private org.example.service.SelectionPerformanceService selectionPerformanceService;

    @Override
    @Transactional
    public SyncResult syncStrategies(List<String> strategyCodes, Date selectionDate) {
        SyncResult result = new SyncResult();
        result.setTotalStrategies(strategyCodes.size());
        result.setSuccessStrategies(0);
        result.setTotalStocks(0);
        result.setErrors(new ArrayList<>());

        for (String strategyCode : strategyCodes) {
            try {
                // 先删除该策略和日期的旧记录
                selectedStockMapper.deleteByStrategyAndDate(strategyCode, selectionDate);

                // 执行选股逻辑（传入选股日期）
                List<StockSelectionResult> selectionResults = stockSelectionService.selectStocks(strategyCode, selectionDate);

                if (selectionResults != null && !selectionResults.isEmpty()) {
                    // 转换为SelectedStock实体
                    List<SelectedStock> selectedStocks = new ArrayList<>();
                    for (StockSelectionResult sr : selectionResults) {
                        SelectedStock stock = new SelectedStock();
                        stock.setStrategyCode(strategyCode);
                        stock.setStockCode(sr.getStockCode());
                        stock.setStockName(sr.getStockName());
                        stock.setSelectionDate(selectionDate);
                        stock.setRemark(sr.getReason());
                        selectedStocks.add(stock);
                    }

                    // 批量插入
                    int insertCount = selectedStockMapper.batchInsert(selectedStocks);
                    result.setTotalStocks(result.getTotalStocks() + insertCount);
                    result.setSuccessStrategies(result.getSuccessStrategies() + 1);
                } else {
                    result.getErrors().add("策略 " + strategyCode + " 未选出任何股票");
                    result.setSuccessStrategies(result.getSuccessStrategies() + 1);
                }

            } catch (Exception e) {
                result.getErrors().add("策略 " + strategyCode + " 同步失败: " + e.getMessage());
            }
        }

        // 如果选股日期是3天以前，计算涨跌幅
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -3);
        Date threeDaysAgo = calendar.getTime();

        if (selectionDate.before(threeDaysAgo) && result.getTotalStocks() > 0) {
            logger.info("选股日期 {} 是3天以前，开始计算涨跌幅", selectionDate);
            try {
                for (String strategyCode : strategyCodes) {
                    int updated = selectionPerformanceService.calculateAndUpdatePerformanceByStrategyAndDate(
                            strategyCode, selectionDate);
                    logger.info("策略 {} 涨跌幅计算完成，更新 {} 条记录", strategyCode, updated);
                }
            } catch (Exception e) {
                logger.error("计算涨跌幅失败", e);
                result.getErrors().add("计算涨跌幅失败: " + e.getMessage());
            }
        }

        return result;
    }

    @Override
    @Async
    public void syncStrategiesAsync(List<String> strategyCodes, Date selectionDate) {
        logger.info("开始异步同步策略选股结果，策略数量: {}, 选股日期: {}", strategyCodes.size(), selectionDate);
        try {
            SyncResult result = syncStrategies(strategyCodes, selectionDate);
            logger.info("异步同步完成，成功策略数: {}/{}, 总股票数: {}",
                    result.getSuccessStrategies(), result.getTotalStrategies(), result.getTotalStocks());
            if (!result.getErrors().isEmpty()) {
                logger.warn("同步过程中出现错误: {}", result.getErrors());
            }
        } catch (Exception e) {
            logger.error("异步同步策略选股结果失败", e);
        }
    }

    @Override
    public List<SelectedStock> findByStrategyAndDate(String strategyCode, Date selectionDate) {
        return selectedStockMapper.findByStrategyAndDate(strategyCode, selectionDate);
    }

    @Override
    public List<SelectedStock> findLatestByStrategy(String strategyCode) {
        return selectedStockMapper.findLatestByStrategy(strategyCode);
    }

    @Override
    public boolean save(SelectedStock selectedStock) {
        return selectedStockMapper.insert(selectedStock) > 0;
    }

    @Override
    public int batchSave(List<SelectedStock> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return selectedStockMapper.batchInsert(list);
    }

    @Override
    public boolean update(SelectedStock selectedStock) {
        return selectedStockMapper.updateById(selectedStock) > 0;
    }

    @Override
    public SelectedStock findById(Long id) {
        return selectedStockMapper.findById(id);
    }

    @Override
    public List<SelectedStock> findAll() {
        return selectedStockMapper.findAll();
    }

    @Override
    public List<SelectedStock> findByDateRange(Date startDate, Date endDate) {
        return selectedStockMapper.findByDateRange(startDate, endDate);
    }

    @Override
    public PageResult<SelectedStock> findByPage(String strategyCode, Date selectionDate, int page, int size) {
        // 计算偏移量
        int offset = (page - 1) * size;

        // 查询数据
        List<SelectedStock> data = selectedStockMapper.findByPage(strategyCode, selectionDate, offset, size);

        // 统计总数
        long total = selectedStockMapper.countByCondition(strategyCode, selectionDate);

        return new PageResult<>(data, total, page, size);
    }
}
