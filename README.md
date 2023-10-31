# jasyncdebug
Debugging problem with using [lmax disruptor](https://github.com/LMAX-Exchange/disruptor) with [jdbc-postgresql](https://mvnrepository.com/artifact/org.postgresql/postgresql). Demonstration utilizes Java 11, lmax 4.0.0, and jdbc postgresql 42.6.0

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
jasyncdebug    | [Thread-0] INFO org.rhodey.poc.psql.PsqlRunnable - psql test thread returned: 123
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
The key demonstration here is that [DisruptorHandler.java is blocking forever on this line](https://github.com/rhodey/jasyncdebug/blob/jdbc/src/main/java/org/rhodey/poc/disruptor/DisruptorHandler.java#L39)
```
jasyncdebug    | [Thread-0] INFO org.rhodey.poc.psql.PsqlRunnable - psql test thread returned: 123
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - CHAN test_read
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - COUNT 1
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - before redis.publish()
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - after redis.publish()
jasyncdebug    | [disruptor-0] INFO org.rhodey.poc.disruptor.DisruptorHandler - before psql.prepareStatement()
jasyncdebug    | [redis-write-1] INFO org.rhodey.poc.disruptor.DisruptorHandler - redis write success
```

## Additional Detail of Interest
As with the previous jasync example a working test of r2dbc plus threading is included:
  + [here](https://github.com/rhodey/jasyncdebug/blob/jdbc/src/main/java/org/rhodey/poc/psql/PsqlService.java#L33)
  + [and here](https://github.com/rhodey/jasyncdebug/blob/jdbc/src/main/java/org/rhodey/poc/psql/PsqlRunnable.java#L23)

## License
MIT - Copyright 2023 mike@rhodey.org