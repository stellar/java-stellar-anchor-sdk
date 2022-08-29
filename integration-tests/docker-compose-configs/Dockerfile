FROM openjdk:11-jdk AS build
WORKDIR /code
COPY . .
RUN pwd && ls
RUN ./gradlew clean build -x test --stacktrace

FROM ubuntu:20.04

RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-11-jre

COPY --from=build /code/service-runner/build/libs/anchor-platform-runner*.jar /app/anchor-platform-runner.jar

COPY --from=build /code/integration-tests/docker-compose-configs/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT ["/app/docker-entrypoint.sh"]
