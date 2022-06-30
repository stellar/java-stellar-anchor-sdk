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
ENV STELLAR_ANCHOR_CONFIG=file:/anchor_config/anchor-config.yaml

#COPY anchor-reference-server/src/main/resources/anchor-reference-server.yaml /config/reference-config.yaml
#COPY platform/src/main/resources/anchor-config-defaults.yaml /config/anchor-config.yaml
#COPY platform/src/main/resources/sep1/stellar-wks.toml /anchor_config/stellar-wks.toml
ENV REFERENCE_SERVER_CONFIG_ENV=file:/config/reference-config.yaml

EXPOSE 8080 8081

#ENTRYPOINT ["java", "-jar", "/app/anchor-platform-runner.jar"]
