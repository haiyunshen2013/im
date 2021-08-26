package com.wish.im.common.message;

import com.wish.im.common.toolkit.Sequence;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/**
 * 消息体
 *
 * @author shy
 * @since 2021/7/22
 */
@Data
@Builder
public class Message {
    private static final Sequence sequence = new Sequence();
    /**
     * 消息id
     */
    private String id;

    @Nullable
    private String originId;

    /**
     * 消息来源
     */
    private String fromId;

    /**
     * 消息目的
     */
    private String toId;

    /**
     * 消息类型
     *
     * @see MsgType
     */
    private int type = MsgType.SEND;

//    private long timestamp = System.currentTimeMillis();
    /**
     * @see MsgStatus
     */
    private int status;

    private String method;

    private String url;

    /**
     * 是否允许服务端存储
     */
    private boolean enableCache;


    private byte[] body;

    public Message() {
        this.id = Long.toString(sequence.nextId());
    }

    private Message(String id, @Nullable String originId, String fromId, String toId, int type, int status, String method, String url, boolean enableCache, byte[] body) {
        this();
        this.originId = originId;
        this.fromId = fromId;
        this.toId = toId;
        if (type == 0) {
            this.type = MsgType.SEND;
        } else {
            this.type = type;
        }
        this.status = status;
        this.method = method;
        this.url = url;
        this.enableCache = enableCache;
        this.body = body;
    }
}
