package org.example.service;

/**
 * 技术指标计算和维护服务
 */
public interface TechnicalIndicatorService {

    /**
     * 计算并更新所有股票的技术指标（均线和量比）
     * @return 更新的记录数
     */
    int calculateAndUpdateAllIndicators();

    /**
     * 计算并更新指定股票的技术指标
     * @param stockCode 股票代码
     * @return 更新的记录数
     */
    int calculateAndUpdateIndicators(String stockCode);
}
