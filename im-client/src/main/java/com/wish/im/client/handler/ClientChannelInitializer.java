package com.wish.im.client.handler;

import com.wish.im.client.NettyClient;
import com.wish.im.common.codec.JsonDecoder;
import com.wish.im.common.codec.JsonEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * 客户端消息处理管道
 *
 * @author shy
 * @date 2021/7/22
 */
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyClient client;

    public ClientChannelInitializer(NettyClient client) {
        this.client = client;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //获取管道
        ChannelPipeline pipeline = ch.pipeline();

        SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        pipeline.addLast(sslCtx.newHandler(ch.alloc()));

        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(100 * 1024,
                0, 4, 0, 4));
        pipeline.addLast(new JsonDecoder());
        pipeline.addLast(new JsonEncoder());
        pipeline.addLast(new IdleStateHandler(60, 20, 60 * 10, TimeUnit.SECONDS));
        pipeline.addLast(new HeartbeatHandler(client));
        pipeline.addLast(new ClientHandler(client));
//        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
    }
}
