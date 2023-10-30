package org.rhodey.poc.disruptor;

import com.lmax.disruptor.EventHandler;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.rhodey.poc.redis.RedisWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

public class DisruptorHandler implements EventHandler<DisruptorEvent> {

    private static final boolean DO_ERROR = System.getenv("do_error").equals("true");
    private static final Logger log = LoggerFactory.getLogger(DisruptorHandler.class);

    private final RedisWriteService redis;
    private final ConnectionFactory psql;
    private long count = 0;

    public DisruptorHandler(RedisWriteService redis, ConnectionFactory psql) {
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
            log.info("before psql.create()");

            BiFunction<Row, RowMetadata, String> mapper = (row, rowMetadata) -> {
                log.info("psql success - returned: " + row.get(0));
                return "please do not make me use io.projectreactor";
            };

            Mono.from(psql.create())
                    .flatMap(c -> Mono.from(c.createStatement("select 456").execute()))
                    .flatMap(result -> Mono.from(result.map(mapper))).subscribe();

            log.info("after psql.create()");
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
