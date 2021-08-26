package com.wish.im.client.handler;

import com.wish.im.client.ImClient;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgStatus;
import com.wish.im.common.message.MsgType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/26
 */
@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<Message> {
    private final ImClient client;

    public ClientHandler(ImClient client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        // 处理响应消息，心跳响应，发送消息响应，请求响应，握手响应
        if (StringUtils.isNotBlank(msg.getOriginId())) {
            ctx.fireChannelRead(msg);
        }
        if (msg.getType() == MsgType.SEND) {
            // 普通消息
            // 1.发送回执
            sendAck(msg);
            // 2.回调
            client.onMessageReceive(msg);
            System.err.println(msg + " --> " + new String(msg.getBody()));
        } else if (msg.getType() == MsgType.HEART || msg.getType() == MsgType.SHAKEHANDS) {
            // ignore HEART
        } else if (msg.getType() == MsgType.ACK) {
            // 2.回调
            client.onMessageReceive(msg);
            // 消息确认
            if (msg.getStatus() == MsgStatus.SERVER_ACK.getValue()) {
                if (log.isDebugEnabled()) {
                    log.debug("消息[{}]服务端已收到", msg.getOriginId());
                }
            } else if (msg.getStatus() == MsgStatus.RECEIVER_ACK.getValue()) {
                if (log.isDebugEnabled()) {
                    log.debug("消息[{}]客户端已收到", msg.getOriginId());
                }
            } else if (msg.getStatus() == MsgStatus.FAIL.getValue()) {
                if (log.isDebugEnabled()) {
                    log.debug("消息[{}]服务端已收到,但接受端离线，本次发送失败", msg.getOriginId());
                }
            }
        } else {
            // 2.回调
            client.onMessageReceive(msg);
            if (msg.getBody() != null) {
                log.info("response : [{}]", new String(msg.getBody()));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("exceptionCaught", cause);
        client.reconnect(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        client.reconnect(ctx.channel());
    }

    private void sendAck(Message msg) {
        Message ack = Message.builder().enableCache(true).status(MsgStatus.RECEIVER_ACK.getValue())
                .toId(msg.getFromId()).originId(msg.getId()).type(MsgType.ACK).build();
        client.sendMsg(ack);
    }
}
