package com.wish.im.common.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 消息体
 *
 * @author shy
 * @since 2021/7/22
 */
@Data
@AllArgsConstructor
public class Message {

    private Header header;

    private byte[] body;

    /**
     * 消息id
     */
    private String id;

    @Nullable
    private String originId;

    public Message() {
        this.id = UUID.randomUUID().toString().replace("-", "");
    }

    public Message(@NotNull Header header, byte[] body) {
        this();
        this.header = header;
        this.body = body;
    }

    /**
     * 消息头
     */
    @Data
    @AllArgsConstructor
    public static class Header {
        public Header() {
        }

        /**
         * 消息类型
         *
         * @see MsgType
         */
        private int msgType = MsgType.SEND;

        /**
         * http协议的content-type
         */
        private String msgContentType;
        /**
         * 消息来源
         */
        private String fromId;
        /**
         * 消息目的
         */
        private String toId;

        private long timestamp = System.currentTimeMillis();
        /**
         * 消息状态
         * 0:尚未发送
         * 1:服务端已接收
         * 2:目标已接收
         * 3:未登录
         * 4:令牌过期
         */
        private int status;

        private String method;

        private String url;

        /**
         * 是否允许服务端存储
         */
        private boolean enableCache;
    }
}
