package org.rhodey.poc.psql;

import io.r2dbc.spi.*;
import org.rhodey.poc.util.Service;

import java.util.concurrent.*;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

public class PsqlService implements Service {
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    private ConnectionFactory connections;

    @Override
    public CompletableFuture<Void> shutdownFuture() {
        return shutdownFuture;
    }

    public ConnectionFactory getConnections() {
        return connections;
    }

    @Override
    public void start() {
        String host = System.getenv("psql_host");
        String port = System.getenv("psql_port");
        String user = System.getenv("psql_user");
        String pass = System.getenv("psql_pass");
        String db = System.getenv("psql_db");

        connections = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "postgresql")
                .option(HOST, host)
                .option(PORT, Integer.parseInt(port))
                .option(USER, user)
                .option(PASSWORD, pass)
                .option(DATABASE, db)
                .build());

        Thread testThread = new Thread(new PsqlRunnable(connections));
        testThread.setDaemon(true);
        testThread.start();
    }

    @Override
    public boolean shutdown(Throwable e) {
        if (!shutdownFuture.isDone()) {
            if (e != null) {
                return shutdownFuture.completeExceptionally(e);
            } else {
                return shutdownFuture.complete(null);
            }
        }
        return false;
    }
}
