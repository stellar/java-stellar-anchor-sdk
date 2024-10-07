ARG BASE_IMAGE=gradle:7.6.4-jdk17-alpine

FROM ${BASE_IMAGE} AS build
WORKDIR /code
COPY --chown=gradle:gradle . .

RUN gradle clean bootJar --stacktrace -x test

FROM ubuntu:22.04

RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-17-jre

COPY --from=build /code/service-runner/build/libs/anchor-platform-runner*.jar /app/anchor-platform-runner.jar
COPY --from=build /code/scripts/docker-start.sh /app/start.sh
COPY --from=build /code/assets /app/assets

WORKDIR /app

ENTRYPOINT ["/bin/bash", "start.sh"]