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
     * 涨停多方炮 - 涨停-跌停-涨停的股票
     */
    UP_DOWN_UP("UP_DOWN_UP", " 涨停-跌停-涨停策略"),

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
    MA_BULLISH("MA_BULLISH", "均线多头排列策略"),

    /**
     * 均线粘合策略 - 五日线拐头向上且接近十日线和二十日线
     * 创业板：过去90天最大涨幅>15%，股价低于半年最高价10%以上
     * 主板：过去60天有连续涨停，股价低于半年最高价20%以上
     */
    MA_CONVERGENCE("MA_CONVERGENCE", "均线粘合策略"),

    /**
     * 破五收十策略 - 最近20天内有过连续涨停，之后股价开始跌破五日线，最近一个交易日收红，收盘价在十日线之上
     */
    BREAK_MA5_CLOSE_MA10("BREAK_MA5_CLOSE_MA10", "破五收十策略"),

    /**
     * 收敛三角形策略 - 一段时间内高点不断降低，低点不断上升，形成收敛三角形形态
     */
    CONVERGING_TRIANGLE("CONVERGING_TRIANGLE", "收敛三角形策略");

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
