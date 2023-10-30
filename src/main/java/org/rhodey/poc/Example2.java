package org.rhodey.poc;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WaitStrategy;
import org.rhodey.poc.disruptor.DisruptorEvent;
import org.rhodey.poc.disruptor.DisruptorHandler;
import org.rhodey.poc.disruptor.DisruptorService;
import org.rhodey.poc.psql.PsqlService;
import org.rhodey.poc.redis.RedisWriteService;
import org.rhodey.poc.util.ShutdownProcedure;

import java.util.LinkedList;
import java.util.List;

public class Example2 {

  private void run() {
    PsqlService psql = new PsqlService();
    RedisWriteService redisWrite = new RedisWriteService();

    List<EventHandler> processors = new LinkedList<>();
    processors.add(new DisruptorHandler(redisWrite, psql.getConnection()));

    WaitStrategy strategy = new BlockingWaitStrategy();
    DisruptorService disruptor = new DisruptorService(strategy, processors.toArray(new EventHandler[processors.size()]));

    // RedisReadService redisRead = new RedisReadService(disruptor.buffer());
    ShutdownProcedure shutdown = new ShutdownProcedure(redisWrite, psql, disruptor);

    try {

      disruptor.start();
      psql.start();
      redisWrite.start();

    long sequence = disruptor.buffer().next();
    DisruptorEvent next = disruptor.buffer().get(sequence);
    next.setChannel("test1");
    next.setMessage("test11");
    disruptor.buffer().publish(sequence);

    next = disruptor.buffer().get(sequence);
    next.setChannel("test2");
    next.setMessage("test22");
    disruptor.buffer().publish(sequence);

    } catch (Exception e) {
      shutdown.shutdown(e);
    }
  }

  public static void main(String[] args) {
    new Example2().run();
  }

}
