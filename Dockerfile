FROM openjdk:11-jdk AS build
# build jar
ADD . /code
WORKDIR /code
RUN ./gradlew clean bootJar --stacktrace

FROM ubuntu:20.04

RUN apt-get update && \
apt-get install -y --no-install-recommends \
        openjdk-11-jre

#temporary for testing reece
RUN apt-get install -y net-tools
RUN apt-get install -y wget

RUN mkdir /app
COPY --from=build /code/service-runner/build/libs/anchor-platform-runner*.jar /app/anchor-platform-runner.jar

RUN mkdir /config
ENV STELLAR_ANCHOR_CONFIG=file:/config/anchor-config.yaml

ENV REFERENCE_SERVER_CONFIG_ENV=file:/config/reference-config.yaml

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "/app/anchor-platform.jar"]
