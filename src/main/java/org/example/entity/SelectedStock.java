package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 已选股票实体类
 */
public class SelectedStock {

    private Long id;

    /** 策略代码 */
    private String strategyCode;

    /** 股票代码 */
    private String stockCode;

    /** 股票名称 */
    private String stockName;

    /** 选股日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date selectionDate;

    /** 选股日收盘价 */
    private BigDecimal selectionClosePrice;

    /** 次日开盘价（买入价） */
    private BigDecimal nextDayOpenPrice;

    /** 备注 */
    private String remark;

    /** 1天后涨跌幅（相对次日开盘价） */
    private BigDecimal gain1day;

    /** 3天后涨跌幅（相对次日开盘价） */
    private BigDecimal gain3day;

    /** 7天后涨跌幅（相对次日开盘价） */
    private BigDecimal gain7day;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }

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

    public Date getSelectionDate() {
        return selectionDate;
    }

    public void setSelectionDate(Date selectionDate) {
        this.selectionDate = selectionDate;
    }

    public BigDecimal getSelectionClosePrice() {
        return selectionClosePrice;
    }

    public void setSelectionClosePrice(BigDecimal selectionClosePrice) {
        this.selectionClosePrice = selectionClosePrice;
    }

    public BigDecimal getNextDayOpenPrice() {
        return nextDayOpenPrice;
    }

    public void setNextDayOpenPrice(BigDecimal nextDayOpenPrice) {
        this.nextDayOpenPrice = nextDayOpenPrice;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public BigDecimal getGain1day() {
        return gain1day;
    }

    public void setGain1day(BigDecimal gain1day) {
        this.gain1day = gain1day;
    }

    public BigDecimal getGain3day() {
        return gain3day;
    }

    public void setGain3day(BigDecimal gain3day) {
        this.gain3day = gain3day;
    }

    public BigDecimal getGain7day() {
        return gain7day;
    }

    public void setGain7day(BigDecimal gain7day) {
        this.gain7day = gain7day;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
