FROM ubuntu:20.04
RUN mkdir /config
ENV STELLAR_ANCHOR_CONFIG=file:/config/anchor-config.yaml
ENV REFERENCE_SERVER_CONFIG_ENV=file:/config/reference-config.yaml
ADD ./copy_config.sh /copy_config.sh 
RUN chmod +x /copy_config.sh
ADD ./anchor_config.yaml /config_files/anchor_config.yaml
ADD ./stellar.toml /config_files/stellar_wks.toml 
RUN echo $(ls -1 /config_files)
RUN echo $(ls -1 /)
WORKDIR "/docs/deployment-examples/aws-fargate-ecs"
ENTRYPOINT ["/copy_config.sh"]
