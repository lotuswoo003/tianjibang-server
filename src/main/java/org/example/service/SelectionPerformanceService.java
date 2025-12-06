package org.example.service;

import java.util.Date;

/**
 * 选股表现计算服务
 */
public interface SelectionPerformanceService {

    /**
     * 计算并更新指定选股记录的涨跌幅
     * 以选股次日开盘价为基准，计算1天后、3天后、7天后的涨跌幅
     *
     * @param selectedStockId 已选股票ID
     * @return 是否更新成功
     */
    boolean calculateAndUpdatePerformance(Long selectedStockId);

    /**
     * 批量计算并更新指定日期的所有选股记录的涨跌幅
     *
     * @param selectionDate 选股日期
     * @return 更新记录数
     */
    int calculateAndUpdatePerformanceByDate(Date selectionDate);

    /**
     * 批量计算并更新指定策略和日期的选股记录的涨跌幅
     *
     * @param strategyCode 策略代码
     * @param selectionDate 选股日期
     * @return 更新记录数
     */
    int calculateAndUpdatePerformanceByStrategyAndDate(String strategyCode, Date selectionDate);
}
