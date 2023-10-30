package org.rhodey.poc.disruptor;

public class DisruptorEvent {
  private String channel;
  private String message;

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
