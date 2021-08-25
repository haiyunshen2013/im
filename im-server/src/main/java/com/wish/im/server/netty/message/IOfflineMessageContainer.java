package com.wish.im.server.netty.message;

import com.wish.im.common.message.Message;

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
     * 清除失效消息
     */
    default void clean() {
    }
}
