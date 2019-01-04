package org.spf4j.jaxrs.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.concurrent.ContextPropagatingExecutorService;
import org.spf4j.concurrent.DefaultContextAwareExecutor;

/**
 *
 * @author Zoltan Farkas
 */
public class ContextPropagatingCompletionStage<T>
        extends CompletableFuture<T>
        implements CompletionStage<T> {

  private final ExecutionContext parentContext;

  private final CompletableFuture<T> cs;

  private final long deadlinenanos;

  public ContextPropagatingCompletionStage(final CompletableFuture<T> cs,
          final ExecutionContext parentContext, final long deadlinenanos) {
    this.parentContext = parentContext;
    this.cs = cs;
    this.deadlinenanos = deadlinenanos;
  }

  @Override
  public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
    return new ContextPropagatingCompletionStage(cs.thenApply(ExecutionContexts.propagatingFunction(fn, parentContext,
            null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
    return new ContextPropagatingCompletionStage(cs.thenApplyAsync(ExecutionContexts.propagatingFunction(fn, parentContext,
            null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
    return new ContextPropagatingCompletionStage(cs.thenApplyAsync(ExecutionContexts.propagatingFunction(fn, parentContext,
            null, deadlinenanos), executor), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
    return new ContextPropagatingCompletionStage(cs.thenAccept(ExecutionContexts.propagatingConsumer(action,
            parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
    return new ContextPropagatingCompletionStage(cs.thenAcceptAsync(ExecutionContexts.propagatingConsumer(action,
            parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
    return new ContextPropagatingCompletionStage(cs.thenAcceptAsync(ExecutionContexts.propagatingConsumer(action,
            parentContext, null, deadlinenanos), executor), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> thenRun(Runnable action) {
    return new ContextPropagatingCompletionStage(
            cs.thenRun(ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos)),
            parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> thenRunAsync(Runnable action) {
    return new ContextPropagatingCompletionStage(cs.thenRunAsync(
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos)),
            parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
    return new ContextPropagatingCompletionStage(
            cs.thenRunAsync(ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos), executor),
            parentContext, deadlinenanos);
  }

  @Override
  public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other,
          BiFunction<? super T, ? super U, ? extends V> fn) {
    return new ContextPropagatingCompletionStage(cs.thenCombine(other,
            ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
          BiFunction<? super T, ? super U, ? extends V> fn) {
    return new ContextPropagatingCompletionStage(cs.thenCombineAsync(other,
            ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos)),
            parentContext, deadlinenanos);
  }

  @Override
  public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
          BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
    return new ContextPropagatingCompletionStage(
            cs.thenCombineAsync(other, ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos),
                    executor), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
          BiConsumer<? super T, ? super U> action) {
    return new ContextPropagatingCompletionStage(
            cs.thenAcceptBoth(other, ExecutionContexts.propagatingBiConsumer(action, parentContext,
                    null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
          BiConsumer<? super T, ? super U> action) {
    return new ContextPropagatingCompletionStage(cs.thenAcceptBothAsync(other,
            ExecutionContexts.propagatingBiConsumer(action, parentContext,
                    null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
          BiConsumer<? super T, ? super U> action, Executor executor) {
    return new ContextPropagatingCompletionStage(cs.thenAcceptBothAsync(other,
            ExecutionContexts.propagatingBiConsumer(action, parentContext,
                    null, deadlinenanos), executor), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
    return new ContextPropagatingCompletionStage(cs.runAfterBoth(other,
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos)),
            parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
    return new ContextPropagatingCompletionStage(cs.runAfterBothAsync(other, ExecutionContexts.propagatingRunnable(action, parentContext,
            null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
    return new ContextPropagatingCompletionStage(cs.runAfterBothAsync(other, ExecutionContexts.propagatingRunnable(action, parentContext,
            null, deadlinenanos), executor), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
    return new ContextPropagatingCompletionStage(
            cs.applyToEither(other,
                    ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos)),
            parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
    return new ContextPropagatingCompletionStage(cs.applyToEitherAsync(other,
            ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other,
          Function<? super T, U> fn, Executor executor) {
    return new ContextPropagatingCompletionStage(cs.applyToEitherAsync(other,
            ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos), executor),
            parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
    return new ContextPropagatingCompletionStage(cs.acceptEither(other,
            ExecutionContexts.propagatingConsumer(action, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
    return new ContextPropagatingCompletionStage(cs.acceptEitherAsync(other,
            ExecutionContexts.propagatingConsumer(action, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other,
          Consumer<? super T> action, Executor executor) {
    return new ContextPropagatingCompletionStage(cs.acceptEitherAsync(other,
            ExecutionContexts.propagatingConsumer(action, parentContext, null, deadlinenanos), executor), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
    return new ContextPropagatingCompletionStage(cs.runAfterEither(other,
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
    return new ContextPropagatingCompletionStage(cs.runAfterEitherAsync(other,
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
    return new ContextPropagatingCompletionStage(cs.runAfterEitherAsync(other,
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos), executor), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
    return new ContextPropagatingCompletionStage(
            cs.thenCompose(ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
    return new ContextPropagatingCompletionStage(
            cs.thenComposeAsync(ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
          Executor executor) {
    return new ContextPropagatingCompletionStage(
            cs.thenComposeAsync(ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos), executor), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
    return new ContextPropagatingCompletionStage(cs.exceptionally(ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
    return new ContextPropagatingCompletionStage(cs.whenComplete(ExecutionContexts.propagatingBiConsumer(action, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
    return new ContextPropagatingCompletionStage(
            cs.whenCompleteAsync(ExecutionContexts.propagatingBiConsumer(action, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
    return new ContextPropagatingCompletionStage(
            cs.whenCompleteAsync(ExecutionContexts.propagatingBiConsumer(action, parentContext, null, deadlinenanos),
            executor), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
    return new ContextPropagatingCompletionStage(
            cs.handle(ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos)), parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
    return new ContextPropagatingCompletionStage(cs.handleAsync(
            ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos)),
            parentContext, deadlinenanos);
  }

  @Override
  public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
    return new ContextPropagatingCompletionStage(cs.handleAsync(
            ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos), executor),
            parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<T> toCompletableFuture() {
    return this;
  }

  @Override
  public String toString() {
    return cs.toString();
  }

  @Override
  public int getNumberOfDependents() {
    return cs.getNumberOfDependents();
  }

  @Override
  public void obtrudeException(Throwable ex) {
    cs.obtrudeException(ex);
  }

  @Override
  public void obtrudeValue(T value) {
    cs.obtrudeValue(value);
  }

  @Override
  public boolean isCompletedExceptionally() {
    return cs.isCompletedExceptionally();
  }

  @Override
  public boolean isCancelled() {
    return cs.isCancelled();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return cs.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean completeExceptionally(Throwable ex) {
    return cs.completeExceptionally(ex);
  }

  @Override
  public boolean complete(T value) {
    return cs.complete(value);
  }

  @Override
  public T getNow(T valueIfAbsent) {
    return cs.getNow(valueIfAbsent);
  }

  @Override
  public T join() {
    return cs.join();
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return cs.get(timeout, unit);
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    try {
      long timeout = deadlinenanos - TimeSource.nanoTime();
      if (timeout < 0) {
        throw new UncheckedTimeoutException("deadline exceeded " + Timing.getCurrentTiming()
                .fromNanoTimeToInstant(deadlinenanos));
      }
      return cs.get(timeout, TimeUnit.NANOSECONDS);
    } catch (TimeoutException ex) {
      throw new UncheckedTimeoutException(ex);
    }
  }

  @Override
  public boolean isDone() {
    return cs.isDone();
  }

  @Override
  public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
    return new ContextPropagatingCompletionStage<T>(cs.completeOnTimeout(value, timeout, unit),
            parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
    return new ContextPropagatingCompletionStage<T>(cs.orTimeout(timeout, unit),
            parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
    return new ContextPropagatingCompletionStage<T>(
            cs.completeAsync(ExecutionContexts.propagatingSupplier(supplier, parentContext, null, deadlinenanos)),
            parentContext, deadlinenanos);
  }

  @Override
  public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
    return new ContextPropagatingCompletionStage<T>(
            cs.completeAsync(ExecutionContexts.propagatingSupplier(supplier, parentContext, null, deadlinenanos),
                    executor),
            parentContext, deadlinenanos);
  }

  @Override
  public CompletionStage<T> minimalCompletionStage() {
    return this;
  }

  @Override
  public CompletableFuture<T> copy() {
    return new ContextPropagatingCompletionStage<>(cs.copy(), parentContext, deadlinenanos);
  }

  @Override
  public Executor defaultExecutor() {
    return DefaultContextAwareExecutor.instance();
  }

  @Override
  public <U> CompletableFuture<U> newIncompleteFuture() {
    return new ContextPropagatingCompletionStage<>(cs.newIncompleteFuture(), parentContext, deadlinenanos);
  }



}
