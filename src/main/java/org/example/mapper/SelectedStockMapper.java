package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.SelectedStock;

import java.util.Date;
import java.util.List;

/**
 * 已选股票Mapper
 */
@Mapper
public interface SelectedStockMapper {

    /**
     * 插入已选股票
     */
    int insert(SelectedStock selectedStock);

    /**
     * 批量插入已选股票
     */
    int batchInsert(@Param("list") List<SelectedStock> list);

    /**
     * 根据策略代码和日期查询
     */
    List<SelectedStock> findByStrategyAndDate(@Param("strategyCode") String strategyCode,
                                               @Param("selectionDate") Date selectionDate);

    /**
     * 根据策略代码查询（最新日期）
     */
    List<SelectedStock> findLatestByStrategy(@Param("strategyCode") String strategyCode);

    /**
     * 根据ID更新
     */
    int updateById(SelectedStock selectedStock);

    /**
     * 更新涨跌幅数据
     */
    int updatePerformance(SelectedStock selectedStock);

    /**
     * 根据ID查询
     */
    SelectedStock findById(@Param("id") Long id);

    /**
     * 查询所有
     */
    List<SelectedStock> findAll();

    /**
     * 根据日期范围查询
     */
    List<SelectedStock> findByDateRange(@Param("startDate") Date startDate,
                                         @Param("endDate") Date endDate);

    /**
     * 删除指定策略和日期的记录
     */
    int deleteByStrategyAndDate(@Param("strategyCode") String strategyCode,
                                 @Param("selectionDate") Date selectionDate);

    /**
     * 分页查询已选股票
     * @param strategyCode 策略代码（可选）
     * @param selectionDate 选股日期（可选）
     * @param offset 偏移量
     * @param limit 每页数量
     */
    List<SelectedStock> findByPage(@Param("strategyCode") String strategyCode,
                                    @Param("selectionDate") Date selectionDate,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    /**
     * 统计符合条件的记录数
     * @param strategyCode 策略代码（可选）
     * @param selectionDate 选股日期（可选）
     */
    long countByCondition(@Param("strategyCode") String strategyCode,
                          @Param("selectionDate") Date selectionDate);
}
