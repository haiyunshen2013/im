package com.wish.im.client.concurrent;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;

/**
 * A {@link org.springframework.util.concurrent.ListenableFuture} whose value can be set via {@link #set(Object)}
 * or {@link #setException(Throwable)}. It may also get cancelled.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.SettableFuture}.
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Mattias Severson
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
public class SettableListenableFuture<T> implements ListenableFuture<T> {

    private static final Callable<Object> DUMMY_CALLABLE = () -> {
        throw new IllegalStateException("Should never be called");
    };


    private final SettableTask<T> settableTask = new SettableTask<>();


    /**
     * Set the value of this future. This method will return {@code true} if the
     * value was set successfully, or {@code false} if the future has already been
     * set or cancelled.
     *
     * @param value the value that will be set
     * @return {@code true} if the value was successfully set, else {@code false}
     */
    public boolean set(@Nullable T value) {
        return this.settableTask.setResultValue(value);
    }

    /**
     * Set the exception of this future. This method will return {@code true} if the
     * exception was set successfully, or {@code false} if the future has already been
     * set or cancelled.
     *
     * @param exception the value that will be set
     * @return {@code true} if the exception was successfully set, else {@code false}
     */
    public boolean setException(Throwable exception) {
        return this.settableTask.setExceptionResult(exception);
    }


    @Override
    public void addCallback(@NotNull ListenableFutureCallback<? super T> callback) {
        this.settableTask.addCallback(callback);
    }

    @Override
    public void addCallback(@NotNull SuccessCallback<? super T> successCallback, @NotNull FailureCallback failureCallback) {
        this.settableTask.addCallback(successCallback, failureCallback);
    }

    @Override
    public @NotNull CompletableFuture<T> completable() {
        return this.settableTask.completable();
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = this.settableTask.cancel(mayInterruptIfRunning);
        if (cancelled && mayInterruptIfRunning) {
            interruptTask();
        }
        return cancelled;
    }

    @Override
    public boolean isCancelled() {
        return this.settableTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.settableTask.isDone();
    }

    /**
     * Retrieve the value.
     * <p>This method returns the value if it has been set via {@link #set(Object)},
     * throws an {@link java.util.concurrent.ExecutionException} if an exception has
     * been set via {@link #setException(Throwable)}, or throws a
     * {@link java.util.concurrent.CancellationException} if the future has been cancelled.
     *
     * @return the value associated with this future
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {
        return this.settableTask.get();
    }

    /**
     * Retrieve the value.
     * <p>This method returns the value if it has been set via {@link #set(Object)},
     * throws an {@link java.util.concurrent.ExecutionException} if an exception has
     * been set via {@link #setException(Throwable)}, or throws a
     * {@link java.util.concurrent.CancellationException} if the future has been cancelled.
     *
     * @param timeout the maximum time to wait
     * @param unit    the unit of the timeout argument
     * @return the value associated with this future
     */
    @Override
    public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.settableTask.get(timeout, unit);
    }

    /**
     * Subclasses can override this method to implement interruption of the future's
     * computation. The method is invoked automatically by a successful call to
     * {@link #cancel(boolean) cancel(true)}.
     * <p>The default implementation is empty.
     */
    protected void interruptTask() {
    }


    private static class SettableTask<T> extends ListenableFutureTask<T> {

        @Nullable
        private volatile Thread completingThread;

        @SuppressWarnings("unchecked")
        public SettableTask() {
            super((Callable<T>) DUMMY_CALLABLE);
        }

        public boolean setResultValue(@Nullable T value) {
            set(value);
            return checkCompletingThread();
        }

        public boolean setExceptionResult(Throwable exception) {
            setException(exception);
            return checkCompletingThread();
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                // Implicitly invoked by set/setException: store current thread for
                // determining whether the given result has actually triggered completion
                // (since FutureTask.set/setException unfortunately don't expose that)
                this.completingThread = Thread.currentThread();
            }
            super.done();
        }

        private boolean checkCompletingThread() {
            boolean check = (this.completingThread == Thread.currentThread());
            if (check) {
                this.completingThread = null;  // only first match actually counts
            }
            return check;
        }
    }

}