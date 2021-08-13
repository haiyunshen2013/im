package com.wish.im.client.concurrent;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Extend {@link Future} with the capability to accept completion callbacks.
 * If the future has completed when the callback is added, the callback is
 * triggered immediately.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ListenableFuture}.
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface ListenableFuture<T> extends Future<T> {

    /**
     * Register the given {@code ListenableFutureCallback}.
     *
     * @param callback the callback to register
     */
    void addCallback(ListenableFutureCallback<? super T> callback);

    /**
     * Java 8 lambda-friendly alternative with success and failure callbacks.
     *
     * @param successCallback the success callback
     * @param failureCallback the failure callback
     * @since 4.1
     */
    void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback);


    /**
     * Expose this {@link org.springframework.util.concurrent.ListenableFuture} as a JDK {@link CompletableFuture}.
     *
     * @since 5.0
     */
    default CompletableFuture<T> completable() {
        CompletableFuture<T> completable = new DelegatingCompletableFuture<>(this);
        addCallback(completable::complete, completable::completeExceptionally);
        return completable;
    }

}