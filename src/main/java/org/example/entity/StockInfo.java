package org.example.entity;

import java.util.Date;

/**
 * 股票基础信息实体类
 */
public class StockInfo {

    private Long id;

    /** 股票代码 */
    private String stockCode;

    /** 股票名称 */
    private String stockName;

    /** 市场(SH/SZ) */
    private String market;

    /** 上市日期 */
    private Date listDate;

    /** 申万一级行业 */
    private String industryLevel1;

    /** 申万二级行业 */
    private String industryLevel2;

    /** 申万三级行业 */
    private String industryLevel3;

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

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public Date getListDate() {
        return listDate;
    }

    public void setListDate(Date listDate) {
        this.listDate = listDate;
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

    public String getIndustryLevel1() {
        return industryLevel1;
    }

    public void setIndustryLevel1(String industryLevel1) {
        this.industryLevel1 = industryLevel1;
    }

    public String getIndustryLevel2() {
        return industryLevel2;
    }

    public void setIndustryLevel2(String industryLevel2) {
        this.industryLevel2 = industryLevel2;
    }

    public String getIndustryLevel3() {
        return industryLevel3;
    }

    public void setIndustryLevel3(String industryLevel3) {
        this.industryLevel3 = industryLevel3;
    }
}
