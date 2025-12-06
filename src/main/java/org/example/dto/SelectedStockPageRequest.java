package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * 已选股票分页查询请求
 */
@Schema(description = "已选股票分页查询请求")
public class SelectedStockPageRequest {

    @Schema(description = "策略代码，如: CONSECUTIVE_LIMIT_UP", example = "CONSECUTIVE_LIMIT_UP")
    private String strategyCode;

    @Schema(description = "选股日期（格式：yyyy-MM-dd），可选", example = "2025-10-10")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date selectionDate;

    @Schema(description = "页码，从1开始", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页数量", example = "20", defaultValue = "20")
    private Integer size = 20;

    public SelectedStockPageRequest() {
    }

    public SelectedStockPageRequest(String strategyCode, Date selectionDate, Integer page, Integer size) {
        this.strategyCode = strategyCode;
        this.selectionDate = selectionDate;
        this.page = page;
        this.size = size;
    }

    // Getters and Setters
    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }

    public Date getSelectionDate() {
        return selectionDate;
    }

    public void setSelectionDate(Date selectionDate) {
        this.selectionDate = selectionDate;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
