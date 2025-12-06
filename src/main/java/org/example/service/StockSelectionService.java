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
     * 根据策略代码和日期执行选股
     * @param strategyCode 策略代码
     * @param selectionDate 选股日期（基于该日期收盘进行选股）
     * @return 选股结果列表
     */
    List<StockSelectionResult> selectStocks(String strategyCode, java.util.Date selectionDate);

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
     * 涨停-跌停-涨停
     * @return 选股结果列表
     */
    List<StockSelectionResult> findUpDownUpStocks();
    /**
     * 历史二波策略 - 过去一年出现过连续涨停，之后上涨到高点回落调整后再次出现连续涨停
     * @return 选股结果列表
     */
    List<StockSelectionResult> findHistoryTwoWavesStocks();

    /**
     * 均线粘合策略 - 五日线拐头向上且接近十日线和二十日线
     * 创业板：过去90天最大涨幅>15%，股价低于半年最高价10%以上
     * 主板：过去60天有连续涨停，股价低于半年最高价20%以上
     * @return 选股结果列表
     */
    List<StockSelectionResult> findMAConvergenceStocks();

    /**
     * 均线粘合策略（指定日期） - 基于指定日期收盘进行选股
     * @param selectionDate 选股日期
     * @return 选股结果列表
     */
    List<StockSelectionResult> findMAConvergenceStocks(java.util.Date selectionDate);

    /**
     * 破五收十策略 - 最近20天内有过连续涨停，之后股价开始跌破五日线，最近一个交易日收红，收盘价在十日线之上
     * @return 选股结果列表
     */
    List<StockSelectionResult> findBreakMA5CloseMA10Stocks();

    /**
     * 破五收十策略（指定日期） - 基于指定日期收盘进行选股
     * @param selectionDate 选股日期
     * @return 选股结果列表
     */
    List<StockSelectionResult> findBreakMA5CloseMA10Stocks(java.util.Date selectionDate);

    /**
     * 收敛三角形策略 - 一段时间内高点不断降低，低点不断上升，形成收敛三角形形态
     * @return 选股结果列表
     */
    List<StockSelectionResult> findConvergingTriangleStocks();

    /**
     * 收敛三角形策略（指定日期） - 基于指定日期收盘进行选股
     * @param selectionDate 选股日期
     * @return 选股结果列表
     */
    List<StockSelectionResult> findConvergingTriangleStocks(java.util.Date selectionDate);
}
