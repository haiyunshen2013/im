package com.wish.im.client.message;

/**
 * 描述
 *
 * @author shy
 * @date 2021/7/27
 */
public interface Callback<T> {
    /**
     * Callback after the message is delivering.
     *
     * @param result r
     */
    void onDelivering(T result);

    /**
     * Callback after the message is delivered.
     *
     * @param result result
     */
    void onDelivered(T result);

    /**
     * Callback after the message response
     * @param result result
     */
    void onResponse(T result);
}
