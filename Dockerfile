FROM openjdk:11-jdk AS build
WORKDIR /code
COPY . .
RUN ./gradlew clean bootJar --stacktrace

FROM ubuntu:20.04

RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-11-jre ca-certificates

COPY --from=build /code/service-runner/build/libs/anchor-platform-runner*.jar /app/anchor-platform-runner.jar

RUN mkdir /usr/local/share/ca-certificates
COPY .github/kubernetes-ingress-controller-fake-certificate.pem /usr/local/share/ca-certificates
RUN /usr/local/share/ca-certificates

ENTRYPOINT ["java", "-jar", "/app/anchor-platform-runner.jar"]
