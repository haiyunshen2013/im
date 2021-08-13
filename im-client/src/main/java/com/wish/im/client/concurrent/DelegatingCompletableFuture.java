package com.wish.im.client.concurrent;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Extension of {@link CompletableFuture} which allows for cancelling
 * a delegate along with the {@link CompletableFuture} itself.
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Juergen Hoeller
 * @since 5.0
 */
class DelegatingCompletableFuture<T> extends CompletableFuture<T> {

    private final Future<T> delegate;


    public DelegatingCompletableFuture(Future<T> delegate) {
        this.delegate = delegate;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean result = this.delegate.cancel(mayInterruptIfRunning);
        super.cancel(mayInterruptIfRunning);
        return result;
    }

}
