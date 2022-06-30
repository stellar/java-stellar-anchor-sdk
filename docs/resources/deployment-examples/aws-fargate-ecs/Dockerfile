FROM ubuntu:20.04
RUN mkdir /config
ENV STELLAR_ANCHOR_CONFIG=file:/config/anchor-config.yaml
ENV REFERENCE_SERVER_CONFIG_ENV=file:/config/reference-config.yaml
ADD ./anchor_config.yaml /config_files/anchor_config.yaml
ADD ./stellar.toml /config_files/stellar_wks.toml 
RUN echo $(ls -1 /config_files)
WORKDIR "/docs/deployment-examples/aws-fargate-ecs"
