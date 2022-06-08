# Running & Configuring the Application

- [Running &amp; Configuring the Application](#running--configuring-the-application)
  - [Running the Application from Source Code](#running-the-application-from-source-code)
  - [Configuring the Project](#configuring-the-project)
    - [Config Files](#config-files)
      - [Path to Yaml](#path-to-yaml)
        - [Yaml Search](#yaml-search)
    - [Environment variables](#environment-variables)
    - [Event Messaging](#event-messaging)
    - [JVM\-Argument based run\-configuration](#jvm-argument-based-run-configuration)
  - [Docker](#docker)
  - [Running the Application from Docker](#running-the-application-from-docker)

> Created by [gh-md-toc](https://github.com/ekalinin/github-markdown-toc.go)

## Running the Application from Source Code

This section covers how to run the application from source code using the provided Kafka docker image configuration, the Anchor Reference server and the secrets provided in the demo configuration files.

1. Clone this repository: `git clone ssh://git@github.com:stellar/java-stellar-anchor-sdk.git`.
2. Start a Kafka Queue service: `cd docs/docker-examples/kafka && docker compose up`
3. Start the Anchor Reference server: `./gradlew service-runner:bootRun --args=--anchor-reference-server`
    - This would use the default configuration file at [`java-stellar-anchor-sdk/anchor-reference-server/src/main/resources/anchor-reference-server.yaml`], but you can use a custom configuration file by setting the `REFERENCE_SERVER_CONFIG_ENV` environment variable to the path of the configuration file, following the [Path to Yaml](#path-to-yaml) format.
4. Start the Anchor Platform: `./gradlew service-runner:bootRun --args=--sep-server`
    - This would use the default configuration file at [`java-stellar-anchor-sdk/platform/src/main/resources/anchor-config-defaults.yaml`], but you can use a custom configuration file by setting the `STELLAR_ANCHOR_CONFIG` environment variable to the path of the configuration file, following the [Path to Yaml](#path-to-yaml) format.

## Configuring the Project

### Config Files

As mentioned previously, both the Anchor Platform and Anchor Reference server are configured using yaml and they have default conuguration files:

- **Anchor Platform** default config file is located at [`java-stellar-anchor-sdk/platform/src/main/resources/anchor-config-defaults.yaml`].
- **Anchor Reference Server** default config file is located at [`java-stellar-anchor-sdk/anchor-reference-server/src/main/resources/anchor-reference-server.yaml`].

In order to use a custom configuration file, you need to set the `STELLAR_ANCHOR_CONFIG` (for Anchor Platform) or the `REFERENCE_SERVER_CONFIG_ENV` (for Anchor Reference Server) environment variable(s) to the path of the configuration file, following the [Path to Yaml](#path-to-yaml) format.

The default config files are very self-explanatory and contain tons of comments to explain what each configuration option does. If you want to customize the configuration, just copy those files and start modifying them.

#### Path to Yaml

The `path-to-yaml` (configuration file) is defined in the following format:

```text
<path-to-yaml> ::= <type>:<location>
<type> ::= file | url | classpath
<location> ::= <file-location> | <url> | <classpath-resource>
```  

Examples:

- `file:/home/user/.anchor/.anchor-config.yaml`
- `url:https://localhost:8080/.anchor-config.yaml`
- `classpath:anchor-config.yaml`

##### Yaml Search

The Platform configuration loader tries to fetch the configuration from up to three different sources before failing, in the following order:

1. The JVM Option `-Dstellar.anchor.config`, for instance:

    ```shell
    ./gradlew service-runner:bootRun --args=--sep-server -PjvmArgs="-Dstellar.anchor.config=[path-to-yaml]" 
    ```

2. The file `.anchor/anchor-config.yaml` in the user's home directory. If the path of the `yaml` is not specified by the JVM options, the server will search for the `./anchor/anchor-config.yaml` file in the user's home directory.

3. The system Environment Variable `STELLAR_ANCHOR_CONFIG`. If neither the JVM option nor the file `.anchor/anchor-config.yaml` is found, the server will search for the `STELLAR_ANCHOR_CONFIG`, whose default value is `"classpath:/anchor-config-defaults.yaml"`:

    ```shell
    STELLAR_ANCHOR_CONFIG=classpath:/anchor-config-defaults.yaml
    ./gradlew service-runner:bootRun --args=--sep-server
    ```

If all of the above fail, the server will fail with an error.

### Environment variables

Secrets are passed to the Anchor Platform (and Anchor Reference Server) via environment variables, which can be set either through command line or using a `.env` file. A list of supported environment variables and their descriptions can be found at [`example.env`].

### Event Messaging

A message queue is required for the Anchor Platform to publish messages to and for the Anchor to consume messages from.
The default queueType used (in the anchor-config.yaml file) is "kafka", and SQS is also supported.

The default Kafka configuration should work out of the box if you have a running Kafka available at the correct port. The easiest way for that is by using the docker-compose.yaml file located at `docs/docker-examples/kafka/docker-compose.yaml`:

```shell
cd docs/docker-examples/kafka
docker compose up
```

### JVM-Argument based run-configuration

Since Java processes take `-D` arguments as JVM system properties, the path/locator of the `yaml` file can be passed to the process through JVM system properties:

```shell
java -Dstellar.anchor.config=file:/path/to/file.yaml -jar anchor-platform.jar --anchor-reference-server
```

## Docker

Docker Build:

```shell
docker build -t stellar-anchor-platform:latest .
```

Docker Run:

Note: secrets (credentials, tokens, etc...) are passed to the application via environment variables. Use `-e` to pass in
each required environment variables ([Environment Variables](../platform/src/main/resources/example.env))

```shell
docker run -v {/local/path/to/config/file/}:/config -p 8081:8081 stellar-anchor-platform:latest --anchor-reference-server \
-e JWT_SECRET='secret' \
-e SEP10_SIGNING_SEED='SAX3...C3AW5X' \
-e 'CIRCLE_API_KEY=QVBJX0...NjMyZTQ5NWJhNDdlZg==' \
-e PAYMENT_GATEWAY_STELLAR_SECRET_KEY='secret' \
-e POSTGRES_USERNAME='postgres' \
-e POSTGRES_PASSWORD='password'
```

or pass in an .env file

```shell
docker run -v {/local/path/to/config/file/}:/config -p 8081:8081 stellar-anchor-platform:latest --anchor-reference-server \
--env-file ./my-env-file
```

Note: this image can run --sep-server (port: 8080), --anchor-reference-server (port: 8081).

## Running the Application from Docker

You can use docker compose to run the whole infrastructure - Anchor Platform, Reference Server, Kafka, and a Postgres Database. All you need to do is making use of the [docker-compose.yaml](../docker-compose.yaml) available at the root of the project:

```shell
docker compose up
```

It will use the default config files [`java-stellar-anchor-sdk/platform/src/main/resources/anchor-config-defaults.yaml`], [`java-stellar-anchor-sdk/anchor-reference-server/src/main/resources/anchor-reference-server.yaml`] and the default environment variables from [`example.env`].

You can test against this setup by running the end-to-end tests ([end_to_end_test.py](../end-to-end-tests/end_to_end_tests.py)) using localhost:8080 as the domain.

[`java-stellar-anchor-sdk/platform/src/main/resources/anchor-config-defaults.yaml`]: ../platform/src/main/resources/anchor-config-defaults.yaml
[`java-stellar-anchor-sdk/anchor-reference-server/src/main/resources/anchor-reference-server.yaml`]: ../anchor-reference-server/src/main/resources/anchor-reference-server.yaml
[`example.env`]: ../platform/src/main/resources/example.env
[`docs/docker-examples/kafka/docker-compose.yaml`]: ../docs/docker-examples/kafka/docker-compose.yaml
