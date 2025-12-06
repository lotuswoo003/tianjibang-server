package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.CurrentStockData;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface CurrentStockDataMapper {

    /**
     * 插入单条记录
     */
    int insert(CurrentStockData currentStockData);

    /**
     * 批量插入
     */
    int insertBatch(List<CurrentStockData> list);

    /**
     * 检查记录是否存在
     */
    boolean exists(@Param("stockCode") String stockCode, @Param("tradeDate") Date tradeDate);

    /**
     * 查询所有记录
     */
    List<CurrentStockData> selectAll();

    /**
     * 根据股票代码查询
     */
    List<CurrentStockData> selectByStockCode(@Param("stockCode") String stockCode);

    /**
     * 根据日期范围查询
     */
    List<CurrentStockData> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 删除超过1年的旧数据
     */
    int deleteOldData(@Param("beforeDate") Date beforeDate);

    /**
     * 查询指定日期范围内指定股票的交易数据（按日期排序）
     */
    List<CurrentStockData> selectByStockCodeAndDateRange(
            @Param("stockCode") String stockCode,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    /**
     * 查询所有股票代码
     */
    List<String> selectAllStockCodes();

    /**
     * 更新技术指标（均线、量比、换手率）
     */
    int updateTechnicalIndicators(CurrentStockData currentStockData);

    /**
     * 根据股票代码查询股票名称
     */
    String findStockNameByCode(@Param("stockCode") String stockCode);

    /**
     * 查询所有不同的股票代码和名称
     */
    List<Map<String, String>> findDistinctStockCodeAndName();

    /**
     * 查询所有不同的股票代码、名称和行业信息
     */
    List<Map<String, String>> findDistinctStockInfo();

    /**
     * 查询指定股票中未计算技术指标的记录（ma5为null）
     * 按日期升序排序
     */
    List<CurrentStockData> selectUncalculatedByStockCode(@Param("stockCode") String stockCode);

    /**
     * 查询某个股票最后一条已计算指标的记录
     */
    CurrentStockData selectLastCalculatedRecord(@Param("stockCode") String stockCode);

    /**
     * 查询某个股票在指定日期之后未计算指标的记录
     */
    List<CurrentStockData> selectUncalculatedAfterDate(
            @Param("stockCode") String stockCode,
            @Param("afterDate") Date afterDate);

    /**
     * 查询指定日期的所有股票代码和名称（用于股票名称修复）
     */
    List<Map<String, String>> findStockNamesByDate(@Param("tradeDate") Date tradeDate);

    /**
     * 批量更新股票名称
     */
    int batchUpdateStockName(@Param("list") List<Map<String, String>> list);

    /**
     * 根据股票代码更新所有该股票的名称
     */
    int updateStockNameByCode(@Param("stockCode") String stockCode, @Param("stockName") String stockName);

    /**
     * 查询指定股票在指定日期的数据
     */
    CurrentStockData selectByStockCodeAndDate(@Param("stockCode") String stockCode, @Param("tradeDate") Date tradeDate);

    /**
     * 查询指定股票在指定日期之后的第N个交易日的数据
     * @param stockCode 股票代码
     * @param afterDate 起始日期
     * @param offset 偏移量（第几个交易日，从0开始）
     */
    CurrentStockData selectByStockCodeAndDateOffset(@Param("stockCode") String stockCode,
                                                    @Param("afterDate") Date afterDate,
                                                    @Param("offset") int offset);
}
