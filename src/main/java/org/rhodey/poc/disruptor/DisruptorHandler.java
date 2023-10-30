package org.rhodey.poc.disruptor;

import com.lmax.disruptor.EventHandler;
import org.rhodey.poc.redis.RedisWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DisruptorHandler implements EventHandler<DisruptorEvent> {

    private static final boolean DO_ERROR = System.getenv("do_error").equals("true");
    private static final Logger log = LoggerFactory.getLogger(DisruptorHandler.class);

    private final RedisWriteService redis;
    private final Connection psql;
    private long count = 0;

    public DisruptorHandler(RedisWriteService redis, Connection psql) {
        this.redis = redis;
        this.psql = psql;
    }

    private void mockWork() throws SQLException  {
        log.info("before redis.publish()");
        redis.getCommands().publish("test_write", "msg").whenComplete((ok, err) -> {
            if (err != null) {
                log.error("error - redis write error", err);
            } else {
                log.info("redis write success");
            }
        });
        log.info("after redis.publish()");

        if (DO_ERROR) {
            log.info("before psql.prepareStatement()");
            ResultSet results = psql.prepareStatement("select 456").executeQuery();
            results.next();
            log.info("psql success - returned" + results.getInt(1));
            log.info("after psql.prepareStatement()");
        }
    }

    @Override
    public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) throws Exception {
        count++;
        log.info("CHAN " + event.getChannel());
        log.info("COUNT " + count);
        mockWork();
    }
}
