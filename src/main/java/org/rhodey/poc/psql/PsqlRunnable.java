package org.rhodey.poc.psql;

import com.github.jasync.sql.db.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PsqlRunnable implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PsqlRunnable.class);
    private final Connection connection;

    public PsqlRunnable(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        connection.sendPreparedStatement("select 123").whenComplete((ok, err) -> {
            if (err != null) {
                log.error("error - psql query failed", err);
            } else {
                log.info("psql test thread returned: " + ok.getRows().get(0).getInt(0));
            }
        });
    }
}
