# Running & Configuring the Application

- [Running & Configuring the Application](#running--configuring-the-application)
  - [Running the Application from Source Code](#running-the-application-from-source-code)
  - [Configuring the Project](#configuring-the-project)
    - [Config Files](#config-files)
      - [Path to Yaml](#path-to-yaml)
        - [Yaml Search](#yaml-search)
    - [Authorization Between Platform<>Anchor](#authorization-between-platformanchor)
    - [Environment variables](#environment-variables)
    - [Supported Assets](#supported-assets)
    - [Event Messaging](#event-messaging)
    - [JVM-Argument based run-configuration](#jvm-argument-based-run-configuration)
  - [Docker](#docker)
  - [Running the Application from Docker](#running-the-application-from-docker)
  - [Incoming Payments Observer](#incoming-payments-observer)
  - [Metrics](#metrics)

## Running the Application from Source Code

This section covers how to run the application from source code using the provided Kafka docker image configuration, the Anchor Reference server and the secrets provided in the demo configuration files.

1. Clone this repository: `git clone ssh://git@github.com:stellar/java-stellar-anchor-sdk.git`.
2. Start a Kafka Queue service: `cd docs/resources/docker-examples/kafka && docker compose up`
3. Start the Anchor Reference server: `./gradlew service-runner:bootRun --args=--anchor-reference-server`
    - This uses the default configuration file at [`anchor-reference-server.yaml`], but you can use a custom configuration file by setting the `REFERENCE_SERVER_CONFIG_ENV` environment variable to the path of the configuration file, following the [Path to Yaml](#path-to-yaml) format.
4. Start the Anchor Platform: `./gradlew service-runner:bootRun --args=--sep-server`
    - This uses the default configuration file at [`anchor-config-defaults.yaml`], but you can use a custom configuration file by setting the `STELLAR_ANCHOR_CONFIG` environment variable to the path of the configuration file, following the [Path to Yaml](#path-to-yaml) format.

## Configuring the Project

### Config Files

As mentioned previously, both the Anchor Platform and Anchor Reference server are configured using yaml and they have default configuration files:

- **Anchor Platform** default config file is located at [`anchor-config-defaults.yaml`].
- **Anchor Reference Server** default config file is located at [`anchor-reference-server.yaml`].

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

2. The file `.anchor/anchor-config.yaml` in the user's home directory. If the path of the `yaml` is not specified by the JVM options, the server searches for the `./anchor/anchor-config.yaml` file in the user's home directory.

3. The system Environment Variable `STELLAR_ANCHOR_CONFIG`. If neither the JVM option nor the file `.anchor/anchor-config.yaml` is found, the server searches for the `STELLAR_ANCHOR_CONFIG`, whose default value is `"classpath:/anchor-config-defaults.yaml"`:

    ```shell
    STELLAR_ANCHOR_CONFIG=classpath:/anchor-config-defaults.yaml
    ./gradlew service-runner:bootRun --args=--sep-server
    ```

If all of the above fail, the server fails with an error.

### Authorization Between Platform<>Anchor

It's possible to enable/disable authorization headers for requests between Platform and Anchor by editing the `integration-auth` configuration in the Platform config map. You can use different secrets depending on the direction of the requests, i.e. one for `Platform->Anchor` and another for `Anchor->Platform`, and you can choose between the following auth options:
- `JWT_TOKEN`: where a secret is used to create a jwt token in the sender side, and this same secret is used to decode the token in the receiver side. This token is added to the `Authorization` header.
- `API_KEY`: where an API key is added directly to the `X-Api-Key` header.
- `NONE`: where no authorization is used.

### Environment variables

Secrets are passed to the Anchor Platform (and Anchor Reference Server) via environment variables, which can be set either through command line or using a `.env` file. A list of supported environment variables and their descriptions can be found at [`example.env`].

### Supported Assets

The Anchor Platform reads the list of supported assets from a json file whose address is configured in the config file under `app-config.app-assets` and defaults to [`assets-test.json`] ([ref](https://github.com/stellar/java-stellar-anchor-sdk/blob/1f84429f0c5d35cee75445686242643fbd8cffa5/platform/src/main/resources/anchor-config-defaults.yaml#L74)).

### Event Messaging

A message queue is required for the Anchor Platform to publish messages to and for the Anchor to consume messages from.
The default queueType used (in the anchor-config.yaml file) is "kafka", and SQS is also supported.

The default Kafka configuration should work out of the box if you have a running Kafka available at the correct port. The easiest way for that is by using the docker-compose.yaml file located at `docs/resources/docker-examples/kafka/docker-compose.yaml`:

```shell
cd docs/resources/docker-examples/kafka
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
each required environment variables ([Environment Variables](/platform/src/main/resources/example.env))

```shell
docker run -v {/local/path/to/config/file/}:/config -p 8081:8081 stellar-anchor-platform:latest --anchor-reference-server \
-e JWT_SECRET='secret' \
-e SEP10_SIGNING_SEED='SAX3...C3AW5X' \
-e CIRCLE_API_KEY='QVBJX0...NjMyZTQ5NWJhNDdlZg==' \
-e PAYMENT_GATEWAY_STELLAR_SECRET_KEY='secret' \
-e POSTGRES_USERNAME='postgres' \
-e POSTGRES_PASSWORD='password'
```

or pass in an .env file

```shell
docker run -v {/local/path/to/config/file/}:/config -p 8081:8081 stellar-anchor-platform:latest --anchor-reference-server \
--env-file ./my-env-file
```

> Note 1: this image can run --sep-server (port: 8080), --anchor-reference-server (port: 8081).

> Note 2: to check all the available environment variables, please refer to the [`anchor-config-defaults.yaml`] file.

## Running the Application with Docker Compose

You can use docker compose to run the whole infrastructure - Anchor Platform, Reference Server, Kafka, and a Postgres Database. All you need to do is making use of the [docker-compose.yaml](/docker-compose.yaml) available at the root of the project:

```shell
docker compose up
```

It uses the default config files [`anchor-config-defaults.yaml`], [`anchor-reference-server.yaml`] and the default environment variables from [`example.env`].

You can test against this setup by running the end-to-end tests ([end_to_end_test.py](/end-to-end-tests/end_to_end_tests.py)) using localhost:8080 as the domain.

## Incoming Payments Observer

The default configuration of the project uses a Stellar network observer to identify incoming Stellar payments. In case the Anchor relies on Circle, it should configure the project to use the Circle Payment Observer. For more information on how to do that, please refer to the [01.B - Circle Payment Observer](/docs/01%20-%20Running%20%26%20Configuring%20the%20Application/B%20-%20Circle%20Payment%20Observer.md) section.


## Metrics
The Anchor Platform exposes a Prometheus metrics endpoint at `<host>:8082/actuator/prometheus`. All standard Spring 
Boot Actuator metrics are enabled by default. There are certain metrics that periodically poll the database (eg: for 
the count of transactions in each state); these metrics are disabled by default. 
They can be enabled with the following configs:

```text
  metrics-service:
    optionalMetricsEnabled: true    # optional metrics that periodically query the database
    runInterval: 30                 # interval to query the database to generate the optional metrics
```

A Grafana dashboard for the Anchor Platform can be found at `docs/resources/grafana-dashboard/anchor-platform-grafana-dashboard.json`
and imported into your Grafana instance to visualized the Prometheus metrics.


[`anchor-config-defaults.yaml`]: /platform/src/main/resources/anchor-config-defaults.yaml
[`anchor-reference-server.yaml`]: /anchor-reference-server/src/main/resources/anchor-reference-server.yaml
[`example.env`]: /platform/src/main/resources/example.env
[`docs/resources/docker-examples/kafka/docker-compose.yaml`]: /docs/resources/docker-examples/kafka/docker-compose.yaml
[`assets-test.json`]: /platform/src/main/resources/assets-test.json

## Logging

The format of anchor platform's logs can be set by the `LOG_APPENDER` environment variable. Supported values include:
* `console_appender`: `timestamp - level - location - message`
* `console_json_appender`: json of the format below

```json
{
    "time": timestamp,
    "source": logger,
    "index": location,
    "event": {
        "message": message,
        "severity": level,
    }
}
```
