package com.wish.im.common.message;

/**
 * 消息类型
 *
 * @author shy
 * @date 2021/7/22
 */
public class MsgType {
    /**
     * 消息类型 握手消息
     */
    public static final int SHAKEHANDS = 1001;
    /**
     * 心跳消息
     */
    public static final int HEART = 1002;

    /**
     * 发送
     */
    public static final int SEND = 1003;

    /**
     * 回执
     */
    public static final int ACK = 1004;

    /**
     * 请求 管理员功能类型
     */
    public static final int REQUEST = 1005;

    /**
     * 响应 管理员功能类型
     */
    public static final int RESPONSE = 1006;

}
