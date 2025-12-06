package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.StockInfo;

import java.util.List;
import java.util.Map;

/**
 * 股票基础信息Mapper
 */
@Mapper
public interface StockInfoMapper {

    /**
     * 根据股票代码查询
     */
    StockInfo findByStockCode(@Param("stockCode") String stockCode);

    /**
     * 根据股票名称查询
     */
    StockInfo findByStockName(@Param("stockName") String stockName);

    /**
     * 查询所有股票信息（返回Map便于快速查找）
     */
    List<StockInfo> findAll();

    /**
     * 插入股票信息
     */
    int insert(StockInfo stockInfo);

    /**
     * 批量插入
     */
    int batchInsert(@Param("list") List<StockInfo> list);

    /**
     * 更新股票信息
     */
    int update(StockInfo stockInfo);

    /**
     * 根据股票代码更新股票名称
     */
    int updateStockName(@Param("stockCode") String stockCode, @Param("stockName") String stockName);

    /**
     * 删除
     */
    int delete(@Param("id") Long id);

    /**
     * 获取所有股票代码和名称的映射
     */
    Map<String, String> getStockCodeNameMap();
}
