FROM maven:3-eclipse-temurin-11-alpine

MAINTAINER mike@rhodey.org

RUN mkdir /app
WORKDIR /app

COPY pom.xml pom.xml
RUN mvn verify

COPY src/ src/
RUN mvn package

CMD java -jar target/jasyncdebug-0.0.1.jar