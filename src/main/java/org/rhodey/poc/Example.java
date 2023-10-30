package org.rhodey.poc;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WaitStrategy;
import org.rhodey.poc.disruptor.DisruptorHandler;
import org.rhodey.poc.disruptor.DisruptorService;
import org.rhodey.poc.psql.PsqlService;
import org.rhodey.poc.redis.RedisReadService;
import org.rhodey.poc.redis.RedisWriteService;
import org.rhodey.poc.util.ShutdownProcedure;

import java.util.LinkedList;
import java.util.List;

public class Example {

  private void run() {
    PsqlService psql = new PsqlService();
    RedisWriteService redisWrite = new RedisWriteService();

    List<EventHandler> processors = new LinkedList<>();
    processors.add(new DisruptorHandler(redisWrite, psql.getConnections()));

    WaitStrategy strategy = new BlockingWaitStrategy();
    DisruptorService disruptor = new DisruptorService(strategy, processors.toArray(new EventHandler[processors.size()]));

    RedisReadService redisRead = new RedisReadService(disruptor.buffer());
    ShutdownProcedure shutdown = new ShutdownProcedure(redisRead, redisWrite, psql, disruptor);

    try {

      disruptor.start();
      psql.start();
      redisWrite.start();
      redisRead.start();

    } catch (Exception e) {
      shutdown.shutdown(e);
    }
  }

  public static void main(String[] args) {
    new Example().run();
  }

}
