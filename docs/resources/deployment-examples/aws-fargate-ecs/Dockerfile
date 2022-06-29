FROM ubuntu:20.04
RUN apt-get install -y net-tools
RUN apt-get install -y wget
RUN mkdir /config
ENV STELLAR_ANCHOR_CONFIG=file:/config/anchor-config.yaml
ENV REFERENCE_SERVER_CONFIG_ENV=file:/config/reference-config.yaml
WORKDIR "/docs/deployment-examples/aws-fargate-ecs"
