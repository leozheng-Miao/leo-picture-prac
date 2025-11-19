package com.yupi.yupicturebackend.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * @program: yu-picture
 * @description: 图片分表算法实现类
 * @author: Miao Zheng
 * @date: 2025-11-18 14:02
 **/
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    /**
     * @param availableTargetNames 目前可用的分表名 （物理表）
     * @param preciseShardingValue 传过来的计算分片所需的可新信息
     * @return
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {

        String logicTableName = preciseShardingValue.getLogicTableName();
        Long spaceId = preciseShardingValue.getValue();
        // spaceId 为空时， 表明查询所有图片
        if (spaceId == null) {
            return logicTableName;
        }
        //根据 spaceId 动态生成 物理表名称
        String realTableName = "picture_" + spaceId;
        // 如果当前物理表中有刚刚生成的 表名，证明当前空间是旗舰版空间
        // 若没有 则是普通空间 或者 私人空间
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        } else {
            return logicTableName;
        }

    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return new ArrayList<>();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
