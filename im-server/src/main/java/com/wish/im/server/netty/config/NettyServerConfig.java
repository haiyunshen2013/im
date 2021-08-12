package com.wish.im.server.netty.config;


import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wish.im.server.mvc.offlinemessage.service.OfflineMessageService;
import com.wish.im.server.netty.message.DbOfflineMessageContainer;
import com.wish.im.server.netty.message.DefaultOfflineMessageContainer;
import com.wish.im.server.netty.message.IOfflineMessageContainer;
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
    public IOfflineMessageContainer offlineMessageContainer(OfflineMessageService offlineMessageService) {
        return new DbOfflineMessageContainer(offlineMessageService);
    }

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
        return JsonUtils.getMapper();
    }

    @Bean
    public AutoFillHandler autoFillHandler() {
        return new AutoFillHandler();
    }
}
