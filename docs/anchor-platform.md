# Anchor Platform Spring Boot Application

## How To Run
```shell
./gradlew bootRun -PjvmArgs="-Dstellar.anchor.config=[path-to-yaml]"

# An example
./gradlew bootRun -PjvmArgs="-Dstellar.anchor.config=file:/etc/anchor-platform/anchor-config.yaml" 
```

### Note: `path-to-yaml` 

The `path-to-yaml` is defined similarly in the following format. 

```
<path-to-yaml> ::= <type>:<location>
<type> ::= file | url | classpath
<location> ::= <file-location> | <url> | <classpath-resource>
```  

Examples:
* `file:/home/user/.anchor/.anchor-config.yaml`
* `url:https://localhost:8080/.anchor-config.yaml`
* `classpath:anchor-config.yaml`

## Configuration Management
### Overall architecture

[Architecture Diagram](https://lucid.app/publicSegments/view/17b493dd-bbaf-49ca-abcd-6f8abaca0494/image.png)

## Run Configuration

The platform Java process reads configurations in the following order. 

### 1. JVM Options `-Dstellar.anchor.config`
The path of the `yaml` file can be specified by the JVM Option, `-Dstellar.anchor.config`. 

```shell
./gradlew bootRun -PjvmArgs="-Dstellar.anchor.config=[path-to-yaml]" 
```

### 2. System Environment Variable `STELLAR_ANCHOR_CONFIG`
The path of the `yaml` file can be specified by the system environment variable `STELLAR_ANCHOR_CONFIG`.
[An example of the yaml file](../platform/example.anchor-config.yaml).

```shell
export STELLAR_ANCHOR_CONFIG=[path-to-yaml]
./gradlew bootRun -PjvmArgs="-Dstellar.anchor.config=[path-to-yaml]"
```

### 3. `.anchor/anchor-config.yaml` in user's home directory
If the path of the `yaml` is not specified by the JVM options or the system environment variable, the server will 
try to find the `./anchor/anchor-config.yaml` file in the user's home directory. 

### JVM-Argument based run-configuration
Java process takes `-D` arguments as JVM system properties. The path/locator of the `yaml` file can be passed to the process through JVM system properties.

Ex:
```shell
java -Dstellar.anchor.config=file:/path/to/file.yaml -jar anchor-platform.jar --anchor-reference-server
```

### 4. Set Environment variables
Secrets are passed to the Anchor Platform via environment variables. A list of supported environment variables and 
descriptions can be found in [here](../platform/src/main/resources/example.env)


## Docker
Docker Build:
```shell
docker build -t stellar-anchor-platform:latest .
```

Docker Run:

Note: secrets (credentials, tokens, etc...) are passed to the application via environment variables. Use `-e` to pass in
each required environment variables ([Environment Variables](../platform/src/main/resources/example.env) )
```shell
docker run -v {/local/path/to/config/file/}:/config -p 8081:8081 stellar-anchor-platform:latest --anchor-reference-server \
-e JWT_SECRET=secret \
-e SEP10_SIGNING_SEED=SAX3...C3AW5X \
-e CIRCLE_API_KEY=QVBJX0...NjMyZTQ5NWJhNDdlZg== \
-e PAYMENT_GATEWAY_STELLAR_SECRET_KEY=secret \
-e POSTGRES_USERNAME=postgres \
-e POSTGRES_PASSWORD=password
```
Note: this image can run --sep-server (port: 8080), --anchor-reference-server (port: 8081), --payment-observer

## Event Messaging
A message queue is also required for the Anchor Platform to publish messages to (and for the Anchor to consumer from).
The default queueType used (in the anchor-config.yaml file) is "kafka". SQS is also supported. The default Kafka configuration
should work out of the box, just deploy a local Kafka instance using the docker-compose.yaml file located at 
`docs/docker-examples/kafka/docker-compose.yaml`

```shell
cd docs/docker-examples/kafka
docker compose up
```

## Docker Compose
You can use docker compose to run everything - Anchor Platform, Reference Server, Kafka, and a Postgres Database.

Config files for the Anchor Platform and Reference Server are located at 
`anchor-reference-server/src/main/resources/anchor-reference-server-docker-compose-config.yaml` and 
`platform/src/main/resources/anchor-docker-compose-config.yaml`. 

Secrets (environment variables) defined in `platform/src/main/resources/example.env` will be used. 

The docker compose file is at the root of the project and will build the Anchor Platform and Reference Server 
from source before deploying.
```shell
docker compose up
```
You can test against this setup by running the end-to-end tests (./end-to-end-tests/end_to_end_test.py) using localhost:8080 as the domain.