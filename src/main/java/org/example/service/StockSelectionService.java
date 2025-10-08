package org.example.service;

import org.example.dto.StockSelectionResult;
import org.example.enums.StockSelectionStrategy;

import java.util.List;

/**
 * 选股服务接口
 */
public interface StockSelectionService {

    /**
     * 根据策略代码执行选股
     * @param strategyCode 策略代码
     * @return 选股结果列表
     */
    List<StockSelectionResult> selectStocks(String strategyCode);

    /**
     * 根据策略枚举执行选股
     * @param strategy 策略枚举
     * @return 选股结果列表
     */
    List<StockSelectionResult> selectStocks(StockSelectionStrategy strategy);

    /**
     * 连续涨停策略 - 查询最近半年有连续涨停的股票
     * @return 选股结果列表
     */
    List<StockSelectionResult> findConsecutiveLimitUpStocks();

    /**
     * 历史二波策略 - 过去一年出现过连续涨停，之后上涨到高点回落调整后再次出现连续涨停
     * @return 选股结果列表
     */
    List<StockSelectionResult> findHistoryTwoWavesStocks();
}
