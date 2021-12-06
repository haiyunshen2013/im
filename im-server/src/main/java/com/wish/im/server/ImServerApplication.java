package com.wish.im.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/28
 */
@SpringBootApplication
@EnableScheduling
public class ImServerApplication {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ImServerApplication.class);
        springApplication.setApplicationContextFactory(webApplicationType -> new AnnotationConfigApplicationContext());
        ApplicationContext context = springApplication.run(args);
    }
}
