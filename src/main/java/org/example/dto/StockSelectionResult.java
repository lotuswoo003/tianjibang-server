package org.example.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 选股结果DTO
 */
public class StockSelectionResult {

    private String stockCode;
    private String stockName;
    private Date latestTradeDate;
    private BigDecimal latestClosePrice;
    private String industryLevel1;
    private String reason; // 选中原因
    private String detail; // 详细信息

    public StockSelectionResult() {
    }

    public StockSelectionResult(String stockCode, String stockName, Date latestTradeDate,
                                BigDecimal latestClosePrice, String industryLevel1, String reason, String detail) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.latestTradeDate = latestTradeDate;
        this.latestClosePrice = latestClosePrice;
        this.industryLevel1 = industryLevel1;
        this.reason = reason;
        this.detail = detail;
    }

    // Getters and Setters
    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public Date getLatestTradeDate() {
        return latestTradeDate;
    }

    public void setLatestTradeDate(Date latestTradeDate) {
        this.latestTradeDate = latestTradeDate;
    }

    public BigDecimal getLatestClosePrice() {
        return latestClosePrice;
    }

    public void setLatestClosePrice(BigDecimal latestClosePrice) {
        this.latestClosePrice = latestClosePrice;
    }

    public String getIndustryLevel1() {
        return industryLevel1;
    }

    public void setIndustryLevel1(String industryLevel1) {
        this.industryLevel1 = industryLevel1;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
