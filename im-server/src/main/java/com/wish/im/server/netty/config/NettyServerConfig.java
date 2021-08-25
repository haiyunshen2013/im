package com.wish.im.server.netty.config;


import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wish.ipusher.api.handler.AutoFillHandler;
import com.wish.ipusher.api.utils.JsonUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/28
 */
@Configuration
@ComponentScan("com.wish.im.server")
@EnableConfigurationProperties(AppConfig.class)
public class NettyServerConfig {

    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = JsonUtils.getMapper();
        JacksonTypeHandler.setObjectMapper(mapper);
        return mapper;
    }

    @Bean
    public AutoFillHandler autoFillHandler() {
        return new AutoFillHandler();
    }
}
