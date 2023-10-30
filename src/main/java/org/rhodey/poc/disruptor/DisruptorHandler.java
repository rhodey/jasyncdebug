package org.rhodey.poc.disruptor;

import com.github.jasync.sql.db.Connection;
import com.lmax.disruptor.EventHandler;
import org.rhodey.poc.redis.RedisWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private void mockWork() {
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
            log.info("before psql.sendPreparedStatement()");
            psql.sendPreparedStatement("select 456").whenComplete((ok, err) -> {
                if (err != null) {
                    log.error("error - psql query error", err);
                } else {
                    log.info("psql success - returned: " + ok.getRows().get(0).getInt(0));
                }
            });
            log.info("after psql.sendPreparedStatement()");
        }
    }

    @Override
    public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) {
        count++;
        log.info("CHAN " + event.getChannel());
        log.info("COUNT " + count);
        mockWork();
    }
}
