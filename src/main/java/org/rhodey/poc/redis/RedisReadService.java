package org.rhodey.poc.redis;

import com.lmax.disruptor.RingBuffer;
import io.lettuce.core.*;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.rhodey.poc.util.Service;
import org.rhodey.poc.disruptor.DisruptorEvent;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class RedisReadService implements Service {

  private static final long CONNECT_TIMEOUT = 5000;
  private static final long COMMAND_TIMEOUT = 5000;

  private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
  private final RingBuffer<DisruptorEvent> buffer;
  private final String host;

  private RedisClient client;
  private StatefulRedisPubSubConnection<String, String> connection;

  public RedisReadService(RingBuffer<DisruptorEvent> buffer) {
    this.host = "redis://" + System.getenv("redis_read_host");
    this.buffer = buffer;
  }

  @Override
  public CompletableFuture<Void> shutdownFuture() {
    return shutdownFuture;
  }

  @Override
  public void start() {
    SocketOptions socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
            .build();

    ClientOptions clientOptions = ClientOptions.builder()
            .socketOptions(socketOptions)
            .pingBeforeActivateConnection(true)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(COMMAND_TIMEOUT)))
            .build();

    ReadThreadFactory threads = new ReadThreadFactory();
    ClientResources clientResources = DefaultClientResources.builder()
            .threadFactoryProvider((name) -> threads)
            .ioThreadPoolSize(2)
            .computationThreadPoolSize(2)
            .build();

    client = RedisClient.create(clientResources, host);
    client.setOptions(clientOptions);
    client.addListener(new ConnectionListener());
    connection = client.connectPubSub();
    connection.addListener(new ConnectionListener());
    connection.addListener(new RedisSubscriber(buffer));
    RedisPubSubAsyncCommands<String, String> async = connection.async();
    async.psubscribe("test_read*").whenComplete((ok, err) -> {
      if (err != null) { shutdown(new RuntimeException("subscribe failed", err)); }
    });
  }

  @Override
  public boolean shutdown(Throwable e) {
    try {
      boolean res = false;
      if (!shutdownFuture.isDone()) {
        if (e != null) {
          res = shutdownFuture.completeExceptionally(e);
        } else {
          res = shutdownFuture.complete(null);
        }
        if (connection != null) { connection.close(); }
        if (client != null) { client.shutdown(); }
      }
      return res;
    } catch (Exception ee) {
      return shutdownFuture.completeExceptionally(ee);
    }
  }

  private class ConnectionListener extends RedisConnectionStateAdapter {
    @Override
    public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
      shutdown(new RuntimeException("redis read disconnected"));
    }

    @Override
    public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {
      shutdown(new RuntimeException("redis read exception", cause));
    }
  }

  private static class ReadThreadFactory implements ThreadFactory {
    private final AtomicInteger count = new AtomicInteger();
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, "redis-read-" + count.getAndIncrement());
      thread.setDaemon(true);
      return thread;
    }
  }
}
