package org.rhodey.poc.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.jetbrains.annotations.NotNull;
import org.rhodey.poc.util.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DisruptorService implements Service, ExceptionHandler<DisruptorEvent>, EventFactory<DisruptorEvent> {

  private static final int buffer_size = 1024;

  private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
  private final Disruptor<DisruptorEvent> disruptor;
  private final EventHandler[] handlers;

  public DisruptorService(WaitStrategy waitStrategy, EventHandler[] handlers) {
    this.handlers = handlers;
    this.disruptor = new Disruptor<>(
        this, buffer_size, new DisruptorThreadFactory(),
        ProducerType.SINGLE, waitStrategy
    );
  }

  public RingBuffer<DisruptorEvent> buffer() {
    return disruptor.getRingBuffer();
  }

  @Override
  public DisruptorEvent newInstance() {
    return new DisruptorEvent();
  }

  @Override
  public CompletableFuture<Void> shutdownFuture() {
    return shutdownFuture;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void start() {
    disruptor.setDefaultExceptionHandler(this);
    disruptor.handleEventsWith(handlers);
    disruptor.start();
  }

  @Override
  public boolean shutdown(Throwable e) {
    try {
      if (!shutdownFuture.isDone()) {
        disruptor.shutdown();
        if (e != null) {
          return shutdownFuture.completeExceptionally(e);
        } else {
          return shutdownFuture.complete(null);
        }
      }
      return false;
    } catch (Exception ee) {
      return shutdownFuture.completeExceptionally(ee);
    }
  }

  @Override
  public void handleOnStartException(Throwable throwable) {
    shutdown(throwable);
  }

  @Override
  public void handleEventException(Throwable throwable, long sequence, DisruptorEvent event) {
    shutdown(throwable);
  }

  @Override
  public void handleOnShutdownException(Throwable throwable) {
    shutdown(throwable);
  }

  private static class DisruptorThreadFactory implements ThreadFactory {
    private final AtomicInteger count = new AtomicInteger();
    @Override
    public Thread newThread(@NotNull Runnable runnable) {
      Thread thread = new Thread(runnable, "disruptor-" + count.getAndIncrement());
      thread.setDaemon(false);
      return thread;
    }
  }
}
