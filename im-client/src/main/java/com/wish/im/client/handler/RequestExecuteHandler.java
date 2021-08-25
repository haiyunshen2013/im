package com.wish.im.client.handler;

import com.wish.im.client.message.ResponseMessage;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgStatus;
import com.wish.im.common.message.MsgType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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

    private long expireRate = 10 * 1000L;

    private final Timer timer = new Timer();

    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            expireFuture();
        }
    };

    public void init() {
        timer.schedule(task, expireRate);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Message response) throws Exception {
        String originId = response.getOriginId();
        ResponseMessage responseMessage = listeners.get(originId);
        if (responseMessage == null) {
            return;
        }
        Message originMessage = responseMessage.getOriginMessage();
        Message.Header responseHeader = response.getHeader();
        if (responseHeader.getMsgType() == MsgType.ACK) {
            if (originMessage.getHeader().isEnableCache()) {
                if (responseHeader.getStatus() == MsgStatus.SERVER_ACK.getValue()) {
                    responseMessage.getListenableFuture().set(response);
                    listeners.remove(originId);
                }
            } else if (responseHeader.getStatus() == MsgStatus.RECEIVER_ACK.getValue() || responseHeader.getStatus() == MsgStatus.FAIL.getValue()) {
                responseMessage.getListenableFuture().set(response);
                listeners.remove(originId);
            }
        } else {
            responseMessage.getListenableFuture().set(response);
            listeners.remove(originId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        listeners.forEach((k, v) -> v.getListenableFuture().setException(cause));
        listeners.clear();
    }

    public void addFuture(String originMsgId, ResponseMessage responseFuture) {
        listeners.put(originMsgId, responseFuture);
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

    public void setExpireRate(long expireRate) {
        this.expireRate = expireRate;
    }
}