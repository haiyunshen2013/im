package com.wish.im.server.netty.interceptor;

import com.wish.im.common.message.Message;
import io.netty.channel.Channel;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/29
 */
public interface Interceptor {
    /**
     * 服务端回应握手消息前
     *
     * @param msg     客户端发送的带验证信息的握手消息
     * @param channel channel
     * @return true:放行，可认为通过验证，false:连接关闭
     */
    boolean beforeShakeHands(Message msg, Channel channel);


}
