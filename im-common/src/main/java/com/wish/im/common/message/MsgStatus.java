package com.wish.im.common.message;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/26
 */
public enum MsgStatus {
    SERVER_ACK(1, "Server ACK"),
    RECEIVER_ACK(2, "Receiver ACK"),
    NOT_LOGIN(3, "Not Login"),
    TOKEN_EXPIRE(4, "Token Expire"),
    OK(200, "OK"),
    FAIL(400, "Failed"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int value;

    private final String msg;

    MsgStatus(int value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    public int getValue() {
        return value;
    }

    public String getMsg() {
        return msg;
    }
}
