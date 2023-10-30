package org.rhodey.poc.psql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

class PsqlRunnable implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PsqlRunnable.class);
    private final Connection connection;

    public PsqlRunnable(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        try {
            ResultSet results = connection.prepareStatement("select 123").executeQuery();
            results.next();
            log.info("psql test thread returned: " + results.getInt(1));
        } catch (SQLException e) {
            log.error("error - psql query failed", e);
        }
    }
}
