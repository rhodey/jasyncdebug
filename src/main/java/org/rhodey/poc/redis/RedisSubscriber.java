package org.rhodey.poc.redis;

import com.lmax.disruptor.RingBuffer;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import org.rhodey.poc.disruptor.DisruptorEvent;

public class RedisSubscriber extends RedisPubSubAdapter<String, String> {
    private final RingBuffer<DisruptorEvent> buffer;
    private long sequence;

    public RedisSubscriber(RingBuffer<DisruptorEvent> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void message(String pattern, String channel, String message) {
        sequence = buffer.next();
        DisruptorEvent next = buffer.get(sequence);
        next.setChannel(channel);
        next.setMessage(message);
        buffer.publish(sequence);
    }
}
