package com.yupi.yupicturebackend.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlus配置类
 * 用于配置MybatisPlus的相关功能，如分页插件等
 */
@Configuration // 标识该类为配置类，相当于Spring的XML配置文件
@MapperScan("com.yupi.yupicturebackend.mapper") // 扫描指定包下的Mapper接口，将其注册到Spring容器中
public class MybatisPlusConfig {

    /**
     * 添加分页插件 拦截器
     *
     * @return MybatisPlusInterceptor 分页插件拦截器
     */
    @Bean // 将该方法返回的对象注册为Spring容器中的Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor(); // 创建MybatisPlus拦截器实例
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // 如果配置多个插件, 切记分页最后添加
        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
        return interceptor;
    }
}