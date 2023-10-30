package org.rhodey.poc.util;

import java.util.concurrent.CompletableFuture;

public interface Service {

  CompletableFuture<Void> shutdownFuture();

  void start() throws Exception;

  boolean shutdown(Throwable e);

}
