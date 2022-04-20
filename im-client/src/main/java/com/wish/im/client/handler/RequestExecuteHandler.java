package com.wish.im.client.handler;

import com.wish.im.client.concurrent.SettableListenableFuture;
import com.wish.im.client.message.ResponseMessage;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgStatus;
import com.wish.im.common.message.MsgType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * 描述
 *
 * @author shy
 * @since 2021/8/13
 */
@Slf4j
public class RequestExecuteHandler extends SimpleChannelInboundHandler<Message> {
    private final Map<String, ResponseMessage> listeners = new ConcurrentHashMap<>();

    private long expireTime = 30 * 60 * 1000L;

    public void init() {
        //do nothing
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Message response) throws Exception {
        String originId = response.getOriginId();
        ResponseMessage responseMessage = listeners.remove(originId);
        if (responseMessage == null) {
            return;
        }
        Message originMessage = responseMessage.getOriginMessage();
        SettableListenableFuture<Message> listenableFuture = responseMessage.getListenableFuture();
        if (response.getType() == MsgType.ACK) {
            if (originMessage.isEnableCache() && response.getStatus() == MsgStatus.SERVER_ACK.getValue()) {
                listenableFuture.set(response);
            } else if (response.getStatus() == MsgStatus.FAIL.getValue()) {
                listenableFuture.setException(new IllegalStateException("receiver is not online"));
            } else if (response.getStatus() == MsgStatus.RECEIVER_ACK.getValue()) {
                listenableFuture.set(response);
            }
        } else {
            listenableFuture.set(response);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        listeners.forEach((k, v) -> v.getListenableFuture().setException(new IOException("连接已断开")));
        listeners.clear();
    }


    public void addFuture(String originMsgId, ResponseMessage responseFuture) {
        listeners.put(originMsgId, responseFuture);
        // 发送消息时惰性删除过期数据，防止OOM
        expireFuture();
    }


    public void expireFuture() {
        listeners.entrySet().removeIf(next -> {
            boolean delete = next.getValue().getTimestamp() + expireTime < System.currentTimeMillis();
            if (delete) {
                next.getValue().getListenableFuture().setException(new
                        TimeoutException());
            }
            log.debug("message {} has expired ", next.getValue().getOriginMessage());
            return delete;
        });
    }

    public Map<String, ResponseMessage> getListeners() {
        return listeners;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }
}