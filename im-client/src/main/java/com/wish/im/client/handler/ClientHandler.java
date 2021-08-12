package com.wish.im.client.handler;

import com.wish.im.client.NettyClient;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgStatus;
import com.wish.im.common.message.MsgType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

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
        if (header.getMsgType() == MsgType.SEND) {
            System.err.println(msg + " --> " + new String(msg.getBody()));
            sendAck(msg);
        } else if (header.getMsgType() == MsgType.ACK) {
            if (header.getStatus() == MsgStatus.SERVER_ACK.getValue()) {
                if (log.isDebugEnabled()) {
                    log.debug("消息[{}]服务端已收到", msg.getOriginId());
                }
                client.onDelivering(msg);
            } else if (header.getStatus() == MsgStatus.RECEIVER_ACK.getValue()) {
                if (log.isDebugEnabled()) {
                    log.debug("消息[{}]客户端已收到", msg.getOriginId());
                }
                client.onDelivered(msg);
            }
        } else if (header.getMsgType() == MsgType.HEART) {
            // TODO: 2021/8/10 记录
        } else {
            client.onResponse(msg);
            if (msg.getBody() != null) {
                log.info("response : [{}]",new String(msg.getBody()));
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
