package org.rhodey.poc.psql;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

class PsqlRunnable implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PsqlRunnable.class);
    private final ConnectionFactory connections;

    public PsqlRunnable(ConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public void run() {
        BiFunction<Row, RowMetadata, String> mapper = (row, rowMetadata) -> {
            log.info("psql test thread returned: " + row.get(0));
            return "please do not make me use io.projectreactor";
        };

        Mono.from(connections.create())
                .flatMap(c -> Mono.from(c.createStatement("select 123").execute()))
                .flatMap(result -> Mono.from(result.map(mapper))).subscribe();
    }
}
