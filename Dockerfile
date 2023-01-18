FROM openjdk:11-jdk AS build
WORKDIR /code
COPY . .
RUN ./gradlew clean bootJar --stacktrace

FROM ubuntu:20.04

RUN apt-get update && \
    apt-get install -y --no-install-recommends wget openjdk-11-jre

COPY --from=build /code/service-runner/build/libs/anchor-platform-runner*.jar /app/anchor-platform-runner.jar

WORKDIR /app

# can enable datadog agent via JAVA_OPTS env variable, ex:
# JAVA_OPTIONS="${JAVA_OPTIONS} -javaagent:/app/dd-java-agent.jar"
ARG DATADOG_JAVA_AGENT_VERSION=0.75.0
RUN wget --quiet --retry-connrefused --waitretry=1 --read-timeout=10 --timeout=10 --output-document /app/dd-java-agent.jar \
    https://repository.sonatype.org/service/local/repositories/central-proxy/content/com/datadoghq/dd-java-agent/${DATADOG_JAVA_AGENT_VERSION}/dd-java-agent-${DATADOG_JAVA_AGENT_VERSION}.jar && \
    chmod +x /app/dd-java-agent.jar

ENTRYPOINT ["java", "-jar", "/app/anchor-platform-runner.jar"]
