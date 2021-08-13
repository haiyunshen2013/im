package com.wish.im.client.concurrent;


import org.jetbrains.annotations.Nullable;

/**
 * Success callback for a {@link org.springframework.util.concurrent.ListenableFuture}.
 *
 * @param <T> the result type
 * @author Sebastien Deleuze
 * @since 4.1
 */
@FunctionalInterface
public interface SuccessCallback<T> {

    /**
     * Called when the {@link ListenableFuture} completes with success.
     * <p>Note that Exceptions raised by this method are ignored.
     *
     * @param result the result
     */
    void onSuccess(@Nullable T result);

}
