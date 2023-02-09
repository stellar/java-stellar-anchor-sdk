#FROM openjdk:11-jdk AS build
FROM gradle:jdk11-alpine AS build
WORKDIR /code
COPY --chown=gradle:gradle . .
RUN gradle clean bootJar --stacktrace

FROM ubuntu:20.04

RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-11-jre

COPY --from=build /code/service-runner/build/libs/anchor-platform-runner*.jar /app/anchor-platform-runner.jar

ENTRYPOINT ["java", "-jar", "/app/anchor-platform-runner.jar"]