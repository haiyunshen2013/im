package com.wish.im.client.handler;

import com.wish.im.client.ImClient;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/22
 */
@Slf4j
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    private final ImClient client;

    /**
     * 记录心跳回应丢失，当丢失大于3时，主动断线重连
     */
    private final AtomicInteger ac = new AtomicInteger();

    public HeartbeatHandler(ImClient client) {
        this.client = client;
    }

    private static final EventLoopGroup EXECUTORS = new DefaultEventLoopGroup(1);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (ac.get() != 0) {
            ac.set(0);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            super.userEventTriggered(ctx, evt);
            IdleState state = ((IdleStateEvent) evt).state();
            switch (state) {
                case READER_IDLE:
                    // 规定时间内没收到服务端心跳包响应，进行重连操作
                    if (ac.getAndIncrement() > 3) {
                        client.disconnect();
                        client.reconnect(ctx.channel());
                        ac.set(0);
                    }
                    log.warn("读空闲了，当前发生读空闲次数{}", ac.get());
                    break;
                case WRITER_IDLE:
                    if (!client.isAutoHeart()) {
                        return;
                    }
                    // 规定时间内没向服务端发送心跳包，即发送一个心跳包
                    if (heartbeatTask == null) {
                        heartbeatTask = new HeartbeatTask(ctx);
                    }
                    EXECUTORS.execute(heartbeatTask);
                    break;
                default:
                    log.debug("state : {}", state);
            }
        }
    }

    private HeartbeatTask heartbeatTask;

    private class HeartbeatTask implements Runnable {

        private final ChannelHandlerContext ctx;

        public HeartbeatTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (ctx.channel().isActive()) {
                Message heart = Message.builder().toId("server").type(MsgType.HEART).build();
                client.sendMsg(heart);
            }
        }
    }
}