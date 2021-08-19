package com.wish.im.server.netty.handler;

import com.wish.im.common.codec.JsonDecoder;
import com.wish.im.common.codec.JsonEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/28
 */
@AllArgsConstructor
@Component
@Slf4j
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final ServerHandler channelHandler;

    private final ServerDispatcherHandler serverDispatcherHandler;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        //获取管道
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        pipeline.addLast(sslCtx.newHandler(socketChannel.alloc()));

        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(100 * 1024,
                0, 4, 0, 4));
        // 设置超时
        pipeline.addLast(new ReadTimeoutHandler(60));
        pipeline.addLast(new JsonDecoder());
        pipeline.addLast(new JsonEncoder());
        //处理类
        pipeline.addLast(channelHandler);
        pipeline.addLast(serverDispatcherHandler);
        if (log.isDebugEnabled()) {
            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
        }
    }
}
