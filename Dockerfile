FROM gradle:7.6.0-jdk11-alpine AS build
WORKDIR /code
COPY --chown=gradle:gradle . .

RUN gradle clean bootJar --stacktrace -x test

FROM ubuntu:22.04

RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-11-jre

COPY --from=build /code/service-runner/build/libs/anchor-platform-runner*.jar /app/anchor-platform-runner.jar
COPY --from=build /code/scripts/docker-start.sh /app/start.sh

ENTRYPOINT ["/bin/bash", "/app/start.sh"]