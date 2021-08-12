package com.wish.im.server;

import com.wish.im.server.netty.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/28
 */
@SpringBootApplication
@EnableScheduling
public class ImServerApplication {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication springApplication = new SpringApplication(ImServerApplication.class);
        springApplication.setApplicationContextClass(AnnotationConfigApplicationContext.class);
        ApplicationContext context = springApplication.run(args);
        String[] beanDefinitionNames = context.getBeanDefinitionNames();
        System.err.println(Arrays.toString(beanDefinitionNames));
        AppConfig bean = context.getBean(AppConfig.class);
        ThreadGroup currentGroup =
                Thread.currentThread().getThreadGroup();
        int noThreads = currentGroup.activeCount();
        Thread[] lstThreads = new Thread[noThreads];
        currentGroup.enumerate(lstThreads);
        for (int i = 0; i < noThreads; i++)
            System.out.println("线程号：" + i + " = " + lstThreads[i].getName());
    }
}
