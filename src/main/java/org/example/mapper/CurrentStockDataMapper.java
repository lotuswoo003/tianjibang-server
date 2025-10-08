package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.CurrentStockData;

import java.util.Date;
import java.util.List;

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
     * 更新技术指标（均线和量比）
     */
    int updateTechnicalIndicators(@Param("id") Long id,
                                   @Param("ma5") java.math.BigDecimal ma5,
                                   @Param("ma10") java.math.BigDecimal ma10,
                                   @Param("ma20") java.math.BigDecimal ma20,
                                   @Param("volumeRatio") java.math.BigDecimal volumeRatio);
}
