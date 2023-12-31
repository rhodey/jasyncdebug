package org.rhodey.poc.redis;

import io.lettuce.core.*;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.jetbrains.annotations.NotNull;
import org.rhodey.poc.util.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class RedisWriteService implements Service {

  private static final long CONNECT_TIMEOUT = 5000;
  private static final long COMMAND_TIMEOUT = 5000;

  private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
  private final String host;

  private RedisClient client;
  private StatefulRedisPubSubConnection<String, String> connection;
  private RedisPubSubAsyncCommands<String, String> pubSubCommands;

  public RedisWriteService() {
    this.host = "redis://" + System.getenv("redis_write_host");
  }

  @Override
  public CompletableFuture<Void> shutdownFuture() {
    return shutdownFuture;
  }

  public RedisPubSubAsyncCommands<String, String> getCommands() {
    return pubSubCommands;
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

    WriteThreadFactory threads = new WriteThreadFactory();
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
    pubSubCommands = connection.async();
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
      shutdown(new RuntimeException("redis write disconnected"));
    }

    @Override
    public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {
      shutdown(new RuntimeException("redis write exception", cause));
    }
  }

  private static class WriteThreadFactory implements ThreadFactory {
    private final AtomicInteger count = new AtomicInteger();
    @Override
    public Thread newThread(@NotNull Runnable runnable) {
      Thread thread = new Thread(runnable, "redis-write-" + count.getAndIncrement());
      thread.setDaemon(true);
      return thread;
    }
  }
}
