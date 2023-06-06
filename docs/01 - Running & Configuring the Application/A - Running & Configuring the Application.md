# Running & Configuring the Application

- [Running & Configuring the Application](#running--configuring-the-application)
  - [Running the Application from Source Code](#running-the-application-from-source-code)
  - [Configuring the Project](#configuring-the-project)
    - [Config Files](#config-files)
      - [Path to Yaml](#path-to-yaml)
        - [Yaml Search](#yaml-search)
    - [Environment variables](#environment-variables)
    - [Secrets](#secrets)
    - [Authorization Between Platform and Anchor](#authorization-between-platformanchor)
    - [Supported Assets](#supported-assets)
    - [Event Messaging](#event-messaging)
    - [Custody Services](#custody-services)
      - [Custody Server Configuration](#custody-server-configuration)
      - [Anchor Platform Configuration](#anchor-platform-configuration)
      - [Kotlin Reference Server Configuration](#kotlin-reference-server-configuration)
      - [Custody Integrations](#custody-integrations)
        - [Fireblocks Configuration](#fireblocks-configuration)
    - [JVM-Argument based run-configuration](#jvm-argument-based-run-configuration)
  - [Docker](#docker)
  - [Running the Application from Docker](#running-the-application-from-docker)
  - [Incoming Payments Observer](#incoming-payments-observer)
  - [Metrics](#metrics)

## Running the Application from Source Code

This section covers how to run the application from source code using the provided Kafka docker image configuration, 
the Anchor Reference server and the secrets provided in the demo configuration files.

1. Clone this repository: `git clone ssh://git@github.com:stellar/java-stellar-anchor-sdk.git`.
2. Start a Kafka Queue service: `cd docs/resources/docker-examples/kafka && docker compose up`
3. Start the Anchor Reference server: `./gradlew service-runner:bootRun --args=--anchor-reference-server`
    - This uses the default configuration file at [`anchor-reference-server.yaml`], but you can use a custom 
   configuration file by setting the `REFERENCE_SERVER_CONFIG_ENV` environment variable to the path of the 
   configuration file, following the [Path to Yaml](#path-to-yaml) format.
4. Start the Anchor Platform: `./gradlew service-runner:bootRun --args=--sep-server`
    - This step requires you to set up a `STELLAR_ANCHOR_CONFIG`. You can test the application by using the default one 
   with `export STELLAR_ANCHOR_CONFIG=file:<full-path-to-java-stellar-anchor-sdk>/platform/src/main/resources/example.anchor-config.yaml`,  
    - Eventually you'll need to set up your own configuration based on the `anchor-config-default-values.yaml`.
    - You will need to export additional environment variables, depending on your configuration. An example of the 
   variables you may need can be found in [`example.env`]
5. Start the Stellar Observer: `./gradlew service-runner:bootRun --args=--stellar-observer`
    - This also needs the `STELLAR_ANCHOR_CONFIG` previously mentioned.

## Configuring the Project

### Config Files

As mentioned previously, both the Anchor Platform and Anchor Reference server are configured using yaml and they have 
default configuration files:

- **Anchor Platform** default config file is located at [`anchor-config-default-values.yaml`].
- **Anchor Reference Server** default config file is located at [`anchor-reference-server.yaml`].

The default configuration files are very self-explanatory and contain tons of comments to explain what each 
configuration option does. For the **Anchor Platform**, to modify the default configuration you can create a new 
'override' configuration file that will be merged on top of the values in the [`anchor-config-defaults-values.yaml`] file.

In order to use the 'override' configuration file, you need to set the `STELLAR_ANCHOR_CONFIG`
environment variable to the path of the 'override' file ([`example.anchor-config.yaml`] is an example 'override' file)

The `REFERENCE_SERVER_CONFIG_ENV` (for Anchor Reference Server)

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

The Platform configuration loader tries to fetch the configuration from up to three different sources before failing,
in the following order:

1. The JVM Option `-Dstellar.anchor.config`, for instance:

    ```shell
    ./gradlew service-runner:bootRun --args="--sep-server --stellar-observer" -PjvmArgs="-Dstellar.anchor.config=[path-to-yaml]" 
    ```

2. The file `.anchor/anchor-config.yaml` in the user's home directory. If the path of the `yaml` is not specified by
   the JVM options, the server searches for the `./anchor/anchor-config.yaml` file in the user's home directory.

3. The system Environment Variable `STELLAR_ANCHOR_CONFIG`. If neither the JVM option nor the file
   `.anchor/anchor-config.yaml` is found, the server searches for the `STELLAR_ANCHOR_CONFIG`, whose default value is
   `"classpath:/anchor-config-defaults.yaml"`:

    ```shell
    STELLAR_ANCHOR_CONFIG=classpath:/anchor-config-defaults.yaml
    ./gradlew service-runner:bootRun --args="--sep-server --stellar-observer"
    ```

If all of the above fail, the server fails with an error.

### Environment variables
All configuration values mentioned in [`anchor-config-default-values.yaml`] can be set via environment variables. 
To set a value, convert the yaml path of the key to POSIX form and set the value.

Example:

Setting the Event Publisher Type
```text
events:
  publisher:
    type: kafka
```
maps to the environment variable:
```text
EVENTS_PUBLISHER_TYPE=kafka
```

### Secrets
Secrets are passed to the Anchor Platform (and Anchor Reference Server) via environment variables, which can be set 
either through command line or using a `.env` file. A list of secret environment variables and their descriptions
can be found in the [`example.env`] file. The following environment variables are required for the Anchor Platform:

Note: secrets will always start with 'SECRET'

```text
# REQUIRED - The secret key of JWT encryption
SECRET_SEP10_JWT_SECRET=<secret>

# REQUIRED - The private key of the SEP-10 challenge.
# We highly recommend that this private key should not be used to sign any transactions to submit to the Stellar
# network.
SECRET_SEP10_SIGNING_SEED=<secret>

# REQUIRED - JWT secrets used to communicate between Anchor and Platform.
SECRET_CALLBACK_API_AUTH_SECRET=<secret>
SECRET_PLATFORM_API_AUTH_SECRET=<secret>
```



### Authorization Between Platform and Anchor

It's possible to enable/disable authorization headers for requests between Platform and Anchor by editing the 
`auth` configuration in the Platform config map. You can use different secrets depending on the direction 
of the requests, i.e. one for `Platform->Anchor` and another for `Anchor->Platform`, and you can choose between the 
following `auth.type` options:
- `JWT_TOKEN`: where a secret is used to create a jwt token in the sender side, and this same secret is used to decode 
the token in the receiver side. This token is added to the `Authorization` header.
- `API_KEY`: where an API key is added directly to the `X-Api-Key` header.
- `NONE`: where no authorization is used.

The following `auth` secrets are required:
```text
# REQUIRED - JWT secrets used to communicate between Anchor and Platform.
SECRET_CALLBACK_API_AUTH_SECRET=<secret>
SECRET_PLATFORM_API_AUTH_SECRET=<secret>
```

### Supported Assets

The Anchor Platform reads the list of supported assets (to be set in the configuration `override` file) in json form, 
an example can be found in ['example.anchor-config.yaml'] under `assets`. 

### Event Messaging

A message queue is required for the Anchor Platform to publish messages to and for the Anchor to consume messages from.
The default event messaging service (defined in ['anchor-config-defualt-values.yaml']) is "kafka", SQS is also supported.

The default Kafka configuration should work out of the box if you have a running Kafka available at the correct port. 
The easiest way for that is by using the docker-compose.yaml file located at `docs/resources/docker-examples/kafka/docker-compose.yaml`:

```shell
cd docs/resources/docker-examples/kafka
docker compose up
```

### Custody Services

#### Custody Server Configuration

If you want to use an external custody service to store and manage your wallets, then you need to deploy one more service - Custody Server.

This service also needs the `STELLAR_ANCHOR_CONFIG` previously mentioned. By default, `anchor-config-default-values.yaml` 
config file will be used.

Also, now you don't need to deploy Stellar Observer, since this Custody Server will be responsible for its functionality.

Update configuration file of Custody Server with base URL and port.
```yaml
custody_server:
  port: <port> # The listening port of the Custody Server. Default value: 8085
  base_url: <base_url> # The base URL of the Custody Server. Default value: http://localhost:8085
```

Configure authentication type.
```yaml
custody_server:
  auth:
    type: jwt # Type of authentication, that is used, when Platform communicates with Custody Server.
              # Possible options: [api_key, jwt]. Default value: none
```

If you set `api_key` or `jwt` authentication type, then you need to an add environment variable:
```bash
SECRET_CUSTODY_SERVER_AUTH_SECRET
```

Start Custody Server using Gradle or Docker:

```bash
./gradlew service-runner:bootRun --args=--custody-server
docker run stellar-anchor-platform:latest --custody-server
```

#### Anchor Platform Configuration

Update configuration file of Kotlin Reference Server to use `custody` deposit info generator type for SEP24 and SEP31.
```yaml
sep24:
  deposit_info_generator_type: custody # Default value: self
sep31:
  deposit_info_generator_type: custody # Default value: self
```

#### Kotlin Reference Server Configuration

Update configuration file of Kotlin Reference Server to enable custody integration.
```yaml
sep24:
  custodyEnabled: true # Indicates, that custody integration is enabled and payment will be submitted using Custody Service
```

#### Custody Integrations

##### Fireblocks Configuration

Configure Fireblocks workspace:
1. [Configure API Co-Signer](https://support.fireblocks.io/hc/en-us/articles/4581830426268)
2. [Add API User](https://support.fireblocks.io/hc/en-us/articles/4407823826194-Adding-new-API-users)
3. [Configure Webhook URL](https://support.fireblocks.io/hc/en-us/articles/4408110107794-Configuring-Webhook-URLs)
4. [Enable One-Time Address feature](https://support.fireblocks.io/hc/en-us/articles/4409104568338)
5. [Configure Transaction Policy](https://support.fireblocks.io/hc/en-us/articles/5227408755356-About-the-Policy-Editor)

Update configuration file of Custody Server.

```yaml
custody:
  type: fireblocks # Default value: none
  fireblocks:
    vault_account_id: <vault_account_id> # ID of Fireblocks vault account, that will be used for payments
    public_key: <public_key> # Fireblocks public key. Is used to verify a webhook signature
    asset_mappings: <asset_mappings> # Mappings of fireblocks asset codes to stellar asset codes. For example:
                                     # XLM_USDC_T_CEKS stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5
                                     # XLM_USDC_T_CEKS_2 stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5
                                     # Codes should be divided with space and each pair of codes should be in new line
```

Add environment variables.

```bash
SECRET_CUSTODY_FIREBLOCKS_API_KEY # API key, that will be added to JWT token claims. JWT token will be sent in requests to Fireblocks API
SECRET_CUSTODY_FIREBLOCKS_SECRET_KEY # secret key, that is used to sign JWT token
```

### JVM-Argument based run-configuration

Since Java processes take `-D` arguments as JVM system properties, the path/locator of the `yaml` file can be passed to
the process through JVM system properties:

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
docker run -v </local/path/to/config/file>:/config -p 8080:8080 \
-e SECRET_SEP10_JWT_SECRET='secret' \
-e SECRET_SEP10_SIGNING_SEED='SAX3...C3AW5X' \
-e SECRET_PLATFORM_API_AUTH_SECRET=myAnchorToPlatformSecret
-e SECRET_CALLBACK_API_AUTH_SECRET=myPlatformToAnchorSecret
-e SECRET_DATA_USERNAME='postgres' \
-e SECRET_DATA_PASSWORD='password' \
stellar-anchor-platform:latest --sep-server
```

or pass in a .env file

```shell
docker run -v {/local/path/to/config/file/}:/config -p 8080:8080 \
--env-file ./my-env-file stellar-anchor-platform:latest --sep-server
```

> Note 1: this image can run --sep-server (port: 8080), --anchor-reference-server (port: 8081) and --stellar-observer 
(no port needed).

> Note 2: to check all the available environment variables, please refer to the [`anchor-config-default-values.yaml`] file.

## Running the Application with Docker Compose

You can use docker compose to run the whole stack - Anchor Platform, Reference Server, Kafka, and a Postgres 
Database. All you need to do is making use of the [docker-compose.yaml](/docker-compose.yaml) available at the root of 
the project:

```shell
docker compose up
```

It uses the default config files [`anchor-config-default-values.yaml`], [`anchor-reference-server.yaml`] and the default 
environment variables from [`example.env`].


You can test against this setup by running the end-to-end tests 
([end_to_end_test.py](/end-to-end-tests/end_to_end_tests.py)) using localhost:8080 as the domain.

## Incoming Payments Observer

The default configuration of the project uses a Stellar network observer to identify incoming Stellar payments.

## Metrics
The Anchor Platform exposes a Prometheus metrics endpoint at `<host>:8082/actuator/prometheus`. All standard Spring 
Boot Actuator metrics are enabled by default. There are certain metrics that periodically poll the database (eg: for 
the count of transactions in each state); these metrics are disabled by default but can be enabled with the following 
configs:

```yaml
metrics:
  enabled: false
  port: 8082               # port to expose metrics on
  extras_enabled: true     # optional metrics that periodically query the database
  run_interval: 30         # interval (seconds) to query the database for the extra metrics
```

A Grafana dashboard for the Anchor Platform can be found at `docs/resources/grafana-dashboard/anchor-platform-grafana-dashboard.json`
and imported into your Grafana instance to visualized the Prometheus metrics.


[`anchor-config-default-values.yaml`]: ../../platform/src/main/resources/config/anchor-config-default-values.yaml
[`anchor-reference-server.yaml`]: ../../anchor-reference-server/src/main/resources/anchor-reference-server.yaml
[`example.env`]: ../../platform/src/main/resources/example.env
['example.anchor-config.yaml']: ../../platform/src/main/resources/example.anchor-config.yaml
[`docs/resources/docker-examples/kafka/docker-compose.yaml`]: ../../docs/resources/docker-examples/kafka/docker-compose.yaml

## Logging

The format of anchor platform's logs can be set by the `LOG_APPENDER` environment variable. Supported values include:
* `console_appender`: `timestamp - level - location - message`
* `console_json_appender`: json of the format below

```json
{
    "time": "2001-01-09 01:05:01",
    "source": "o.s.a.p.ServiceRunner",
    "index": "location",
    "event": {
        "message": "Starting ServiceRunner using Java 11.0.16...",
        "severity": "DEBUG"
    }
}
```
