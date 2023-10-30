package org.rhodey.poc.psql;

import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.ConnectionPoolConfigurationBuilder;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;
import org.jetbrains.annotations.NotNull;
import org.rhodey.poc.util.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class PsqlService implements Service {
    private static final Logger log = LoggerFactory.getLogger(PsqlService.class);
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    private Connection connection;

    public Connection getConnection() {
        return connection;
    }

    @Override
    public CompletableFuture<Void> shutdownFuture() {
        return shutdownFuture;
    }

    @Override
    public void start() throws Exception {
        ThreadFactory threads = new PsqlThreadFactory();
        ExecutorService exec = new ThreadPoolExecutor(2, 2, 5000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(2), threads);

        String host = System.getenv("psql_host");
        String port = System.getenv("psql_port");
        String user = System.getenv("psql_user");
        String pass = System.getenv("psql_pass");
        String db = System.getenv("psql_db");

        ConnectionPoolConfigurationBuilder config = new ConnectionPoolConfigurationBuilder();
        config.setHost(host);
        config.setPort(Integer.parseInt(port));
        config.setUsername(user);
        config.setPassword(pass);
        config.setDatabase(db);
        config.setMaxPendingQueries(4);
        config.setMaxActiveConnections(2);
        config.setExecutionContext(exec);

        connection = PostgreSQLConnectionBuilder.createConnectionPool(config);
        int test = connection.sendPreparedStatement("select 1").get().getRows().get(0).getInt(0);
        if (test != 1) { throw new RuntimeException("psql connection is invalid"); }

        log.info("psql connection is valid");
        Thread testThread = new Thread(new PsqlRunnable(connection));
        testThread.setDaemon(true);
        testThread.start();
    }

    @Override
    public boolean shutdown(Throwable e) {
        if (!shutdownFuture.isDone()) {
            if (connection != null) { connection.disconnect(); }
            if (e != null) {
                return shutdownFuture.completeExceptionally(e);
            } else {
                return shutdownFuture.complete(null);
            }
        }
        return false;
    }

    private static class PsqlThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    }

}
