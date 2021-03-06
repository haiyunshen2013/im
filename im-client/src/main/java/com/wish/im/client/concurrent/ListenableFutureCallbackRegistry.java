package com.wish.im.client.concurrent;


import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Helper class for {@link ListenableFuture} implementations that maintains a
 * of success and failure callbacks and helps to notify them.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ExecutionList}.
 *
 * @param <T> the callback result type
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ListenableFutureCallbackRegistry<T> {

    private final Queue<SuccessCallback<? super T>> successCallbacks = new LinkedList<>();

    private final Queue<FailureCallback> failureCallbacks = new LinkedList<>();

    private State state = State.NEW;

    @Nullable
    private Object result;

    private final Object mutex = new Object();


    /**
     * Add the given callback to this registry.
     *
     * @param callback the callback to add
     */
    public void addCallback(ListenableFutureCallback<? super T> callback) {
        synchronized (this.mutex) {
            switch (this.state) {
                case NEW:
                    this.successCallbacks.add(callback);
                    this.failureCallbacks.add(callback);
                    break;
                case SUCCESS:
                    notifySuccess(callback);
                    break;
                case FAILURE:
                    notifyFailure(callback);
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void notifySuccess(SuccessCallback<? super T> callback) {
        try {
            callback.onSuccess((T) this.result);
        } catch (Throwable ex) {
            // Ignore
        }
    }

    private void notifyFailure(FailureCallback callback) {
        try {
            callback.onFailure((Throwable) this.result);
        } catch (Throwable ex) {
            // Ignore
        }
    }

    /**
     * Add the given success callback to this registry.
     *
     * @param callback the success callback to add
     * @since 4.1
     */
    public void addSuccessCallback(SuccessCallback<? super T> callback) {
        synchronized (this.mutex) {
            switch (this.state) {
                case NEW:
                    this.successCallbacks.add(callback);
                    break;
                case SUCCESS:
                    notifySuccess(callback);
                    break;
            }
        }
    }

    /**
     * Add the given failure callback to this registry.
     *
     * @param callback the failure callback to add
     * @since 4.1
     */
    public void addFailureCallback(FailureCallback callback) {
        synchronized (this.mutex) {
            switch (this.state) {
                case NEW:
                    this.failureCallbacks.add(callback);
                    break;
                case FAILURE:
                    notifyFailure(callback);
                    break;
            }
        }
    }

    /**
     * Trigger a {@link ListenableFutureCallback#onSuccess(Object)} call on all
     * added callbacks with the given result.
     *
     * @param result the result to trigger the callbacks with
     */
    public void success(@Nullable T result) {
        synchronized (this.mutex) {
            this.state = State.SUCCESS;
            this.result = result;
            SuccessCallback<? super T> callback;
            while ((callback = this.successCallbacks.poll()) != null) {
                notifySuccess(callback);
            }
        }
    }

    /**
     * Trigger a {@link ListenableFutureCallback#onFailure(Throwable)} call on all
     * added callbacks with the given {@code Throwable}.
     *
     * @param ex the exception to trigger the callbacks with
     */
    public void failure(Throwable ex) {
        synchronized (this.mutex) {
            this.state = State.FAILURE;
            this.result = ex;
            FailureCallback callback;
            while ((callback = this.failureCallbacks.poll()) != null) {
                notifyFailure(callback);
            }
        }
    }


    private enum State {NEW, SUCCESS, FAILURE}

}
