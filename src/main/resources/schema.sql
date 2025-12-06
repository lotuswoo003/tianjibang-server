-- 股票数据表（历史数据）
CREATE TABLE IF NOT EXISTS stock_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    stock_name VARCHAR(50) NOT NULL COMMENT '股票名称',
    trade_date DATE NOT NULL COMMENT '交易日期',
    open_price DECIMAL(18,4) COMMENT '开盘价',
    high_price DECIMAL(18,4) COMMENT '最高价',
    low_price DECIMAL(18,4) COMMENT '最低价',
    close_price DECIMAL(18,4) COMMENT '收盘价',
    volume BIGINT COMMENT '成交量(股)',
    turnover DECIMAL(18,4) COMMENT '成交额(元)',
    float_market_value DECIMAL(18,4) COMMENT '流通市值',
    total_market_value DECIMAL(18,4) COMMENT '总市值',
    industry_level1 VARCHAR(50) COMMENT '申万一级行业',
    industry_level2 VARCHAR(50) COMMENT '申万二级行业',
    industry_level3 VARCHAR(50) COMMENT '申万三级行业',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY idx_stock_date (stock_code, trade_date),
    INDEX idx_stock_code (stock_code),
    INDEX idx_trade_date (trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票交易数据表';

-- 当前股票数据表（最近1年数据）
CREATE TABLE IF NOT EXISTS current_stock_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    stock_name VARCHAR(50) NOT NULL COMMENT '股票名称',
    trade_date DATE NOT NULL COMMENT '交易日期',
    open_price DECIMAL(18,4) COMMENT '开盘价',
    high_price DECIMAL(18,4) COMMENT '最高价',
    low_price DECIMAL(18,4) COMMENT '最低价',
    close_price DECIMAL(18,4) COMMENT '收盘价',
    volume BIGINT COMMENT '成交量(股)',
    turnover DECIMAL(18,4) COMMENT '成交额(元)',
    float_market_value DECIMAL(18,4) COMMENT '流通市值',
    total_market_value DECIMAL(18,4) COMMENT '总市值',
    industry_level1 VARCHAR(50) COMMENT '申万一级行业',
    industry_level2 VARCHAR(50) COMMENT '申万二级行业',
    industry_level3 VARCHAR(50) COMMENT '申万三级行业',
    ma5 DECIMAL(18,4) COMMENT '5日均线',
    ma10 DECIMAL(18,4) COMMENT '10日均线',
    ma20 DECIMAL(18,4) COMMENT '20日均线',
    volume_ratio DECIMAL(10,4) COMMENT '量比',
    turnover_rate DECIMAL(10,4) COMMENT '换手率(成交量/流通市值*100)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY idx_stock_date (stock_code, trade_date),
    INDEX idx_stock_code (stock_code),
    INDEX idx_trade_date (trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='当前股票交易数据表（最近1年）';

-- 股票基础信息表
CREATE TABLE IF NOT EXISTS stock_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    stock_name VARCHAR(50) NOT NULL COMMENT '股票名称',
    market VARCHAR(10) COMMENT '市场(SH/SZ)',
    list_date DATE COMMENT '上市日期',
    industry_level1 VARCHAR(50) COMMENT '申万一级行业',
    industry_level2 VARCHAR(50) COMMENT '申万二级行业',
    industry_level3 VARCHAR(50) COMMENT '申万三级行业',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY idx_stock_code (stock_code),
    INDEX idx_stock_name (stock_name),
    INDEX idx_industry_level1 (industry_level1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票基础信息表';

-- 已选股票表
CREATE TABLE IF NOT EXISTS selected_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_code VARCHAR(50) NOT NULL COMMENT '策略代码',
    stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
    stock_name VARCHAR(50) NOT NULL COMMENT '股票名称',
    selection_date DATE NOT NULL COMMENT '选股日期',
    selection_close_price DECIMAL(18,4) COMMENT '选股日收盘价',
    next_day_open_price DECIMAL(18,4) COMMENT '次日开盘价（买入价）',
    remark VARCHAR(500) COMMENT '备注',
    gain_1day DECIMAL(10,4) COMMENT '1天后涨跌幅（相对次日开盘价）',
    gain_3day DECIMAL(10,4) COMMENT '3天后涨跌幅（相对次日开盘价）',
    gain_7day DECIMAL(10,4) COMMENT '7天后涨跌幅（相对次日开盘价）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY idx_strategy_stock_date (strategy_code, stock_code, selection_date),
    INDEX idx_strategy_code (strategy_code),
    INDEX idx_selection_date (selection_date),
    INDEX idx_stock_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已选股票表';
