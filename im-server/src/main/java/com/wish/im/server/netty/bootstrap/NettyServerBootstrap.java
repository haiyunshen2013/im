package com.wish.im.server.netty.bootstrap;

import com.wish.im.server.netty.config.AppConfig;
import com.wish.im.server.netty.handler.BoosHandler;
import com.wish.im.server.netty.handler.ServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 服务端启动入口
 *
 * @author shy
 * @since 2021/7/29
 * //
 */
@Component
@AllArgsConstructor
public class NettyServerBootstrap {

    private final AppConfig appConfig;

    private final ServerChannelInitializer serverChannelInitializer;


    @PostConstruct
    public void start() {
        Thread daemon = new Thread(() -> {
            //boss线程监听端口，worker线程负责数据读写
            EventLoopGroup boss = new NioEventLoopGroup();
            EventLoopGroup worker = new NioEventLoopGroup();

            try {
                //辅助启动类
                ServerBootstrap bootstrap = new ServerBootstrap();
                //设置线程池
                bootstrap.group(boss, worker);

                //设置socket工厂
                bootstrap.channel(NioServerSocketChannel.class).handler(new BoosHandler());

                //设置管道工厂
                bootstrap.childHandler(serverChannelInitializer).handler(new LoggingHandler(LogLevel.INFO));

                //设置TCP参数
                //1.链接缓冲池的大小（ServerSocketChannel的设置）
                bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
                //维持链接的活跃，清除死链接(SocketChannel的设置)
                bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
                //关闭延迟发送
                bootstrap.childOption(ChannelOption.TCP_NODELAY, true);

                //绑定端口
                ChannelFuture future = bootstrap.bind(appConfig.getPort()).sync();
                System.out.println("server start ...... ");

                //等待服务端监听端口关闭
                future.channel().closeFuture().sync();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                //优雅退出，释放线程池资源
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
        });
        daemon.setDaemon(false);
        daemon.start();
    }
}
