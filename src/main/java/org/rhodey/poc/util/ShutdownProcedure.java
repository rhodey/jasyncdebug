package org.rhodey.poc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class ShutdownProcedure {

  private static final Logger log = LoggerFactory.getLogger(ShutdownProcedure.class);

  private final Service[] services;
  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  public ShutdownProcedure(Service... services) {
    this.services = services;
    for (Service service : services) { service.shutdownFuture().whenComplete((ok, err) -> shutdown(err)); }
  }

  public boolean shutdown(Throwable error) {
    if (!shutdown.getAndSet(true)) {
      if (error != null) {
        log.error("error - initiating shutdown procedure", error);
      } else {
        log.info("initiating shutdown procedure");
      }

      for (Service service : services) { service.shutdown(null); }
      return true;
    }
    return false;
  }
}
