package com.wish.im.client.handler;

import com.wish.im.client.NettyClient;
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
    private final NettyClient client;

    public ClientHandler(NettyClient client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Message.Header header = msg.getHeader();
        // 处理响应消息，心跳响应，发送消息响应，请求响应，握手响应
        if (StringUtils.isNotBlank(msg.getOriginId())) {
            ctx.fireChannelRead(msg);
        }
        if (header.getMsgType() == MsgType.SEND) {
            sendAck(msg);
            System.err.println(msg + " --> " + new String(msg.getBody()));
        } else if (header.getMsgType() == MsgType.HEART || header.getMsgType() == MsgType.SHAKEHANDS) {
            // ignore HEART
        } else if (header.getMsgType() == MsgType.ACK) {
            // 消息确认
            if (header.getStatus() == MsgStatus.SERVER_ACK.getValue()) {
                if (log.isDebugEnabled()) {
                    log.debug("消息[{}]服务端已收到", msg.getOriginId());
                }
            } else if (header.getStatus() == MsgStatus.RECEIVER_ACK.getValue()) {
                if (log.isDebugEnabled()) {
                    log.debug("消息[{}]客户端已收到", msg.getOriginId());
                }
            } else if (header.getStatus() == MsgStatus.FAIL.getValue()) {
                if (log.isDebugEnabled()) {
                    log.debug("消息[{}]服务端已收到,但接受端离线，本次发送失败", msg.getOriginId());
                }
            }
        } else {
            if (msg.getBody() != null) {
                log.info("response : [{}]", new String(msg.getBody()));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        client.reconnect();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        client.reconnect();
    }

    private void sendAck(Message msg) {
        Message.Header header = msg.getHeader();
        Message.Header ackHeader = new Message.Header();
        ackHeader.setStatus(MsgStatus.RECEIVER_ACK.getValue());
        ackHeader.setToId(header.getFromId());
        ackHeader.setMsgType(MsgType.ACK);
        Message ackMessage = new Message(ackHeader, null);
        ackMessage.setOriginId(msg.getId());
        client.sendMsg(ackMessage);
    }
}
