package com.wish.im.client;

import com.wish.im.client.concurrent.ListenableFuture;
import com.wish.im.client.concurrent.SettableListenableFuture;
import com.wish.im.client.constants.ClientStatus;
import com.wish.im.client.handler.ClientChannelInitializer;
import com.wish.im.client.handler.RequestExecuteHandler;
import com.wish.im.client.message.Callback;
import com.wish.im.client.message.ResponseMessage;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * netty连接客户端
 *
 * @author shy
 * @since 2021/7/26
 */
@Slf4j
public class ImClient implements Closeable {
    /**
     * 客户端id，全局唯一
     */
    private final String clientId;

    private String token;

    private final String host;

    private final int port;

    private Bootstrap bootstrap;

    private NioEventLoopGroup eventExecutors;

    private Channel channel;

    /**
     * 客户端状态 0：初始化，1：准备中，2：连接成功，3：连接失败
     */
    private volatile int status;

    /**
     * true:重连
     */
    private volatile boolean isReconnecting;

    private boolean autoHeart = true;

    private boolean autoReconnect;

    private Callback<Message> callback;

    public ImClient(String clientId, String host, int port) {
        this.clientId = clientId;
        this.host = host;
        this.port = port;
    }

    /**
     * 初始化连接参数
     */
    private void init() {
        eventExecutors = new NioEventLoopGroup(1);
        bootstrap = new Bootstrap();
        bootstrap.group(eventExecutors).channel(NioSocketChannel.class)
                // 设置该选项以后，如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文
                .option(ChannelOption.SO_KEEPALIVE, true)
                // 设置禁用nagle算法
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ClientChannelInitializer(this));
    }

    /**
     * 建立连接,如果连接失败，则进入重连阶段
     */
    public void connect() {
        status = ClientStatus.CONNECTING;
        while (status != ClientStatus.CONNECTED) {
            init();
            doConnect();
            if (!autoReconnect) {
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void doConnect() {
        try {
            channel = null;
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            channel = channelFuture.channel();
            status = ClientStatus.CONNECTED;
            Message.Header header = new Message.Header();
            header.setMsgType(MsgType.SHAKEHANDS);
            byte[] body = token != null ? token.getBytes(StandardCharsets.UTF_8) : null;
            Message shakeHandsMsg = new Message(header, body);
            sendMsg(shakeHandsMsg);
        } catch (Exception e) {
            e.printStackTrace();
            if (status != ClientStatus.CONNECT_FAIL) {
                status = ClientStatus.CONNECT_FAIL;
            }
        }
    }

    /**
     * 断线重连
     */
    public void reconnect() {
        if (!autoReconnect) {
            return;
        }
        // 双重校验判定是否重连
        if (!isReconnecting) {
            synchronized (this) {
                if (!isReconnecting) {
                    isReconnecting = true;
                    if (channel != null) {
                        channel.disconnect();
                    }
                    connect();
                    isReconnecting = false;
                }
            }
        }
    }

    public ListenableFuture<Message> sendMsg(Message message) {
        message.getHeader().setFromId(clientId);
        SettableListenableFuture<Message> responseFuture = new SettableListenableFuture<>();
        ChannelFuture channelFuture = channel.writeAndFlush(message);
        RequestExecuteHandler requestExecuteHandler = channel.pipeline().get(RequestExecuteHandler.class);
        requestExecuteHandler.addFuture(message.getId(), new ResponseMessage(message, responseFuture));
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (!responseFuture.isDone()) {
                if (!future.isSuccess()) {
                    responseFuture.setException(future.cause());
                    requestExecuteHandler.getListeners().remove(message.getId());
                } else if (message.getHeader().getMsgType() == MsgType.ACK) {
                    // ack消息没有回执，发送成功及完成
                    responseFuture.set(null);
                    requestExecuteHandler.getListeners().remove(message.getId());
                }
            }
        });
        return responseFuture;
    }

    public ListenableFuture<Message> sendMsg(byte[] body, String toId) {
        Message.Header header = new Message.Header();
        header.setMsgType(MsgType.SEND);
        header.setToId(toId);
        Message message = new Message(header, body);
        return sendMsg(message);
    }

    /**
     * 关闭连接，释放资源
     */
    @Override
    public void close() {
        status = 0;
        channel.disconnect();
        boolean shutdown = eventExecutors.isShutdown();
        if (!shutdown) {
            eventExecutors.shutdownGracefully();
        }
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void onMessageReceive(Message message) {
        if (callback != null) {
            try {
                callback.onMessageReceive(message);
            } catch (Exception e) {
                log.error("onMessageReceive error", e);
            }
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getToken() {
        return token;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public NioEventLoopGroup getEventExecutors() {
        return eventExecutors;
    }

    public Channel getChannel() {
        return channel;
    }

    public int getStatus() {
        return status;
    }

    public boolean isReconnecting() {
        return isReconnecting;
    }

    public boolean isAutoHeart() {
        return autoHeart;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public Callback<Message> getCallback() {
        return callback;
    }

    public void setAutoHeart(boolean autoHeart) {
        this.autoHeart = autoHeart;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public ImClient setCallback(Callback<Message> callback) {
        this.callback = callback;
        return this;
    }
}
