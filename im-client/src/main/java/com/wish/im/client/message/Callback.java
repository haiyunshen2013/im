package com.wish.im.client.message;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/27
 */
public interface Callback<T> {
    /**
     * Callback after the message response
     *
     * @param result result
     */
    void onMessageReceive(T result);
}
