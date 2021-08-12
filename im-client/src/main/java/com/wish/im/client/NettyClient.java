package com.wish.im.client;

import com.wish.im.client.constants.ClientStatus;
import com.wish.im.client.handler.ClientChannelInitializer;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.wish.im.common.message.MsgType.*;

/**
 * netty连接客户端
 *
 * @author shy
 * @since 2021/7/26
 */
@Slf4j
@Data
public class NettyClient implements Closeable {
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
     * 是否缓存离线消息
     */
    private boolean isCacheMsgIfOffline;

    /**
     * 客户端状态 0：初始化，1：准备中，2：连接成功，3：连接失败
     */
    private volatile int status;

    /**
     * true:重连
     */
    private volatile boolean isReconnecting;

    private boolean autoHeart;

    private boolean autoReconnect;

    /**
     * 掉线以后保存的离线消息
     */
    private final Set<Message> offLineMsg = new LinkedHashSet<>();

    public NettyClient(String clientId, String host, int port) {
        this.clientId = clientId;
        this.host = host;
        this.port = port;
    }

    public NettyClient setCacheMsgIfOffline(boolean cacheMsgIfOffline) {
        isCacheMsgIfOffline = cacheMsgIfOffline;
        return this;
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
        while (status != ClientStatus.CONNECTED ) {
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
            init();
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            channel = channelFuture.channel();
            status = ClientStatus.CONNECTED;
            isReconnecting = false;
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
        if (!isReconnecting && autoReconnect) {
            isReconnecting = true;
            channel.disconnect();
            connect();
        }
    }

    private void sendOffLineMsg() {
        offLineMsg.forEach(this::doSendMsg);
    }

    public void sendMsg(Message message) {
        message.getHeader().setFromId(clientId);
        if (status != ClientStatus.CONNECTED && isCacheMsgIfOffline) {
            putOfflineMsg(message);
        } else {
            if (message.getHeader().getMsgType() == HEART) {
                sendOffLineMsg();
            }
            doSendMsg(message);
        }
    }

    private void doSendMsg(Message message) {
        ChannelFuture channelFuture = channel.writeAndFlush(message);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                if (isCacheMsgIfOffline) {
                    putOfflineMsg(message);
                }
            } else {
                if (message.getHeader().getMsgType() == ACK) {
                    log.trace("消息[{}]已发送ACK", message.getId());
                } else if (message.getHeader().getMsgType() != HEART  && message.getHeader().getMsgType() != SHAKEHANDS) {
                    log.debug("消息[{}]已发送", message.getId());
                }
            }
        });
    }

    private void putOfflineMsg(Message message) {
        if (message.getHeader().getMsgType() != HEART) {
            offLineMsg.add(message);
        }
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

    public void onDelivering(Message message) {
        Optional<Message> optional = offLineMsg.stream()
                .filter(msg -> StringUtils.equals(msg.getId(), message.getId())).findAny();
        optional.ifPresent(offLineMsg::remove);
    }

    public void onDelivered(Message message) {

    }

    public void onResponse(Message message) {

    }
    public NettyClient setToken(String token) {
        this.token = token;
        return this;
    }
}
