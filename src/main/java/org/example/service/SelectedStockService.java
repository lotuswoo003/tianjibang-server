package org.example.service;

import org.example.entity.SelectedStock;

import java.util.Date;
import java.util.List;

/**
 * 已选股票服务接口
 */
public interface SelectedStockService {

    /**
     * 同步策略选股结果到数据库
     * @param strategyCodes 策略代码列表
     * @param selectionDate 选股日期
     * @return 同步结果统计
     */
    SyncResult syncStrategies(List<String> strategyCodes, Date selectionDate);

    /**
     * 异步同步策略选股结果到数据库
     * @param strategyCodes 策略代码列表
     * @param selectionDate 选股日期
     */
    void syncStrategiesAsync(List<String> strategyCodes, Date selectionDate);

    /**
     * 根据策略代码和日期查询
     */
    List<SelectedStock> findByStrategyAndDate(String strategyCode, Date selectionDate);

    /**
     * 根据策略代码查询最新记录
     */
    List<SelectedStock> findLatestByStrategy(String strategyCode);

    /**
     * 保存已选股票
     */
    boolean save(SelectedStock selectedStock);

    /**
     * 批量保存
     */
    int batchSave(List<SelectedStock> list);

    /**
     * 更新
     */
    boolean update(SelectedStock selectedStock);

    /**
     * 根据ID查询
     */
    SelectedStock findById(Long id);

    /**
     * 查询所有
     */
    List<SelectedStock> findAll();

    /**
     * 根据日期范围查询
     */
    List<SelectedStock> findByDateRange(Date startDate, Date endDate);

    /**
     * 分页查询
     * @param strategyCode 策略代码（可选）
     * @param selectionDate 选股日期（可选）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 分页结果
     */
    PageResult<SelectedStock> findByPage(String strategyCode, Date selectionDate, int page, int size);

    /**
     * 分页结果
     */
    class PageResult<T> {
        private List<T> data;
        private long total;
        private int page;
        private int size;
        private int totalPages;

        public PageResult() {
        }

        public PageResult(List<T> data, long total, int page, int size) {
            this.data = data;
            this.total = total;
            this.page = page;
            this.size = size;
            this.totalPages = (int) Math.ceil((double) total / size);
        }

        public List<T> getData() {
            return data;
        }

        public void setData(List<T> data) {
            this.data = data;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
    }

    /**
     * 同步结果统计
     */
    class SyncResult {
        private int totalStrategies;
        private int successStrategies;
        private int totalStocks;
        private List<String> errors;

        public SyncResult() {
        }

        public SyncResult(int totalStrategies, int successStrategies, int totalStocks, List<String> errors) {
            this.totalStrategies = totalStrategies;
            this.successStrategies = successStrategies;
            this.totalStocks = totalStocks;
            this.errors = errors;
        }

        public int getTotalStrategies() {
            return totalStrategies;
        }

        public void setTotalStrategies(int totalStrategies) {
            this.totalStrategies = totalStrategies;
        }

        public int getSuccessStrategies() {
            return successStrategies;
        }

        public void setSuccessStrategies(int successStrategies) {
            this.successStrategies = successStrategies;
        }

        public int getTotalStocks() {
            return totalStocks;
        }

        public void setTotalStocks(int totalStocks) {
            this.totalStocks = totalStocks;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
}
