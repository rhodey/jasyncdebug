package org.rhodey.poc.psql;

import org.rhodey.poc.util.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.*;

public class PsqlService implements Service {
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    private Connection connection;

    @Override
    public CompletableFuture<Void> shutdownFuture() {
        return shutdownFuture;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void start() throws SQLException {
        String host = System.getenv("psql_host");
        String port = System.getenv("psql_port");
        String user = System.getenv("psql_user");
        String pass = System.getenv("psql_pass");
        String db = System.getenv("psql_db");
        String uri = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        connection = DriverManager.getConnection(uri, user, pass);

        Thread test = new Thread(new PsqlRunnable(connection));
        test.setDaemon(true);
        test.start();
    }

    @Override
    public boolean shutdown(Throwable e) {
        try {
            if (!shutdownFuture.isDone()) {
                if (connection != null) { connection.close(); }
                if (e != null) {
                    return shutdownFuture.completeExceptionally(e);
                } else {
                    return shutdownFuture.complete(null);
                }
            }
            return false;
        } catch (SQLException ee) {
            return shutdownFuture.completeExceptionally(ee);
        }
    }
}
