package org.example.enums;

/**
 * 选股策略枚举
 */
public enum StockSelectionStrategy {

    /**
     * 连续涨停策略 - 查询最近半年有连续涨停的股票
     */
    CONSECUTIVE_LIMIT_UP("CONSECUTIVE_LIMIT_UP", "连续涨停策略"),

    /**
     * 历史二波策略 - 过去一年出现过连续涨停，之后上涨到高点回落调整后再次出现连续涨停
     */
    HISTORY_TWO_WAVES("HISTORY_TWO_WAVES", "历史二波策略"),

    /**
     * 底部放量策略 - 查询底部放量的股票
     */
    BOTTOM_VOLUME("BOTTOM_VOLUME", "底部放量策略"),

    /**
     * 均线多头排列策略 - 查询均线多头排列的股票
     */
    MA_BULLISH("MA_BULLISH", "均线多头排列策略");

    private final String code;
    private final String description;

    StockSelectionStrategy(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取策略
     */
    public static StockSelectionStrategy fromCode(String code) {
        for (StockSelectionStrategy strategy : values()) {
            if (strategy.getCode().equals(code)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("未知的策略代码: " + code);
    }
}
