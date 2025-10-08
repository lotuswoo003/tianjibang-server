package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.StockData;

import java.util.Date;
import java.util.List;

@Mapper
public interface StockDataMapper {

    /**
     * 根据ID查询
     */
    StockData findById(@Param("id") Long id);

    /**
     * 根据股票代码查询最新数据
     */
    StockData findByStockCode(@Param("stockCode") String stockCode);

    /**
     * 查询所有股票数据
     */
    List<StockData> findAll();

    /**
     * 根据股票代码和日期查询
     */
    StockData findByStockCodeAndDate(@Param("stockCode") String stockCode,
                                      @Param("tradeDate") Date tradeDate);

    /**
     * 根据日期范围查询
     */
    List<StockData> findByDateRange(@Param("stockCode") String stockCode,
                                     @Param("startDate") Date startDate,
                                     @Param("endDate") Date endDate);

    /**
     * 根据行业查询
     */
    List<StockData> findByIndustry(@Param("industry") String industry);

    /**
     * 插入股票数据
     */
    int insert(StockData stockData);

    /**
     * 批量插入
     */
    int batchInsert(List<StockData> list);

    /**
     * 更新股票数据
     */
    int update(StockData stockData);

    /**
     * 根据ID删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据股票代码删除
     */
    int deleteByStockCode(@Param("stockCode") String stockCode);
}
