package com.wish.im.server.netty.message;

import com.wish.im.common.message.Message;
import com.wish.im.server.netty.client.ClientInfo;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/27
 */
public interface IOfflineMessageContainer {
    /**
     * 保存离线消息
     *
     * @param pack 消息
     */
    void putOffLienMsg(Message pack);

    /**
     * 移除已发送
     *
     * @param pack 消息
     */
    void removeOfflineMsg(Message pack);

    /**
     * 获取
     *
     * @param key k
     * @param <T> R
     * @return R
     */
    <T> T getOfflineMsgByToId(String key);

    /**
     * 是否已包含此消息
     *
     * @param pack pack
     * @return true：已保存此消息
     */
    boolean containsMsg(Message pack);

    /**
     * 代理转发消息转发消息，如若发送失败，非心跳类消息会暂时保存，等待下一次发送
     *
     * @param msg 原始消息
     * @param to  客户端
     */
    default void transferMsg(Message msg, ClientInfo to) {
        //转发给接受端
        ChannelFuture channelFuture = to.getChannel().writeAndFlush(msg);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess() && msg.getHeader().isEnableCache()) {
                putOffLienMsg(msg);
            } else {
                removeOfflineMsg(msg);
            }
        });
    }

    /**
     * 清除失效消息
     */
    void clean();
}
