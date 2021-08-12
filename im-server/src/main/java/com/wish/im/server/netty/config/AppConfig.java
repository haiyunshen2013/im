package com.wish.im.server.netty.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/28
 */
@Data
@ConfigurationProperties("ipusher.server")
public class AppConfig {

    private int port = 8080;
}
