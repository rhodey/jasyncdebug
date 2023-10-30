# jasyncdebug
Debugging problem with using [lmax disruptor](https://github.com/LMAX-Exchange/disruptor) with [jasync-postgresql](https://github.com/jasync-sql/jasync-sql). Demonstration utilizes Java 11, lmax 4.0.0, and jasync-postgresql 2.1.16

## Configure
```
cp example.env .env
```

## Run With Docker
If you have docker:
```
docker build -t jasyncdebug .
docker-compose up -d redis postgres
docker-compose up jasyncdebug
```

## Run Without Docker
If you dont have docker but you have redis and postgres:
```
mvn package
export $(cat .env | xargs)
java -jar target/jasyncdebug-0.0.1.jar
```

## Write to Redis
To reproduce the error you will need to write messages to redis.
```
echo "publish test_read 123" | nc -N localhost 6379
```

## Output When Config `do_error=false`
```
jasyncdebug    | [main] INFO com.github.jasync.sql.db.util.NettyUtils - jasync selected transport - nio
jasyncdebug    | [main] INFO com.github.jasync.sql.db.pool.ActorBasedObjectPool - registering pool for periodic connection tests com.github.jasync.sql.db.pool.ActorBasedObjectPool@7ec7ffd3 - PoolConfiguration(maxObjects=2, maxIdle=60000, maxQueueSize=4, validationInterval=5000, createTimeout=10000, testTimeout=5000, queryTimeout=null, coroutineDispatcher=Dispatchers.Default, maxObjectTtl=null, minIdleObjects=null)
jasyncdebug    | [main] INFO org.rhodey.poc.psql.PsqlService - psql connection is valid
jasyncdebug    | [Thread-3] INFO org.rhodey.poc.psql.PsqlRunnable - psql test thread returned: 123
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - CHAN test_read
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - COUNT 1
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - before redis.publish()
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - after redis.publish()
jasyncdebug    | [redis-write-1] INFO org.rhodey.poc.disruptor.DisruptorHandler - redis write success
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - CHAN test_read
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - COUNT 2
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - before redis.publish()
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - after redis.publish()
jasyncdebug    | [redis-write-1] INFO org.rhodey.poc.disruptor.DisruptorHandler - redis write success
```

## Output When Config `do_error=true`
The key demonstration here is that [DisruptorHandler.java is blocking forever on this line](https://github.com/rhodey/jasyncdebug/blob/main/src/main/java/org/rhodey/poc/disruptor/DisruptorHandler.java#L34)
```
jasyncdebug    | [main] INFO com.github.jasync.sql.db.util.NettyUtils - jasync selected transport - nio
jasyncdebug    | [main] INFO com.github.jasync.sql.db.pool.ActorBasedObjectPool - registering pool for periodic connection tests com.github.jasync.sql.db.pool.ActorBasedObjectPool@7ec7ffd3 - PoolConfiguration(maxObjects=2, maxIdle=60000, maxQueueSize=4, validationInterval=5000, createTimeout=10000, testTimeout=5000, queryTimeout=null, coroutineDispatcher=Dispatchers.Default, maxObjectTtl=null, minIdleObjects=null)
jasyncdebug    | [main] INFO org.rhodey.poc.psql.PsqlService - psql connection is valid
jasyncdebug    | [Thread-2] INFO org.rhodey.poc.psql.PsqlRunnable - psql test thread returned: 123
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - CHAN test_read
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - COUNT 1
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - before redis.publish()
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - after redis.publish()
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - before psql.sendPreparedStatement()
jasyncdebug    | [redis-write-1] INFO org.rhodey.poc.disruptor.DisruptorHandler - redis write success
```

## Additional Detail of Interest
This repo demonstrates that jasync does not simply have a general problem with thread-safety. Notice how a new thread is created [here](https://github.com/rhodey/jasyncdebug/blob/main/src/main/java/org/rhodey/poc/psql/PsqlService.java#L53) and the debug output from [this line](https://github.com/rhodey/jasyncdebug/blob/main/src/main/java/org/rhodey/poc/psql/PsqlRunnable.java#L21) is present in both output logs above.


## License
MIT - Copyright 2023 mike@rhodey.org