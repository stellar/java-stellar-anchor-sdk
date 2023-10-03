# How to set up the development environment

<!-- TOC -->
* [How to set up the development environment](#how-to-set-up-the-development-environment)
  * [Install JDK 11](#install-jdk-11)
  * [Checkout the Project](#checkout-the-project)
  * [Set up `docker`](#set-up-docker)
  * [Set up your hosts file](#set-up-your-hosts-file)
  * [Build the Project with Gradle](#build-the-project-with-gradle)
    * [Clean](#clean)
    * [Build](#build)
    * [Running Unit Tests](#running-unit-tests)
  * [Set up the Git Hooks](#set-up-the-git-hooks)
* [Set up the Development Environment with IntelliJ IDEA](#set-up-the-development-environment-with-intellij-idea)
  * [Configuring Gradle on IntelliJ IDEA](#configuring-gradle-on-intellij-idea)
  * [Run Configurations](#run-configurations)
    * [Example: Debug the Platform Server](#example-debug-the-platform-server)
    * [Example: Debug the Junit or the End-to-End Tests](#example-debug-the-junit-or-the-end-to-end-tests)
  * [Running the Tests From Gradle in IntelliJ](#running-the-tests-from-gradle-in-intellij)
<!-- TOC -->

## Install JDK 11

Before you start, please make sure you
have [JDK-11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) installed on your machine.

To check if you have it installed, run:

```shell
java -version
```

## Checkout the Project

To check out the project, run:

```shell
git clone git@github.com:stellar/java-stellar-anchor-sdk.git 
```

or

```shell
git clone https://github.com/stellar/java-stellar-anchor-sdk.git
```

## Set up `docker`

Please make sure you have `docker` installed on your machine. If you don't, please follow the instructions on
the [docker website](https://docs.docker.com/get-docker/).

Docker version 23.0.0 or higher is recommended. To check your docker version, run:

```shell
docker --version
```

You should see something like:

```shell
Docker version 23.0.5, build bc4487a
```

## Set up your hosts file

On macOS and Linux, you need to add the following entries to your `/etc/hosts` file. And on Windows, you need to add the
following entries to your `C:\Windows\System32\Drivers\etc\hosts` file.

These entries are needed to successfully run integration tests and end-2-end tests for the docker containers to
communicate with each other.

```shell
127.0.0.1 db
127.0.0.1 zookeeper
127.0.0.1 kafka
127.0.0.1 sep24-reference-ui
127.0.0.1 reference-server
127.0.0.1 wallet-server
127.0.0.1 platform
127.0.0.1 custody-server
127.0.0.1 host.docker.internal
```

## Build the Project with Gradle

This project is wrapped with Gradle version `8.2.1`. You can use the Gradle wrapper to build the project.

### Clean

`./gradlew clean`

### Build

Buile all projects: `./gradlew build`

Build a subproject: `./gradlew :[subproject]:build`

Build the Spring Boot application: `./gradlew bootJar`

### Running Unit Tests

Run all tests: `./gradlew test`

Run subproject tests: `./gradlew :[subproject]:test`

## Set up the Git Hooks

In order to have consistent code style, we use [Google Java Format](https://github.com/google/google-java-format) to
format the code, and [ktfmt](https://github.com/facebook/ktfmt) to format Kotlin code.
Before each commit, you should run the git hook to format the code. We have a script that will set up the git hooks for
you. To install it, run:

```shell
./gradlew updateGitHook
```

The gradle `updateGitHook` task will install the git hooks for you. You should see `spotlessCheck` and `spotlessApply`
being executed when you commit your code.

# Set up the Development Environment with IntelliJ IDEA

The project is mostly developed with IntelliJ, therefore we will only cover the project configuration for that IDE.

## Configuring Gradle on IntelliJ IDEA

1. Clone the repository:

    ```shell
    git clone git@github.com:stellar/java-stellar-anchor-sdk.git
    ```
2. Install IntelliJ
3. Install and configure `google-java-format`
    1. File -> Settings -> Plugins -> Marketplace -> Search for `google-java-format` -> Install
    2. File -> Settings -> google-java-format -> Check `Enable google-java-format` and
       choose `Default Google Java Style` -> Apply
4. Install and configure `ktfmt`
    1. File -> Settings -> Plugins -> Marketplace -> Search for `ktfmt` -> Install
    2. File -> Settings -> Editor -> ktfmt -> Check `Enable ktfmt` and choose `Google (internal)` -> Apply
5. Use IntelliJ to open as a Gradle project:
    1. Launch IntelliJ
    2. File -> Open, choose the folder `java-stellar-anchor-sdk` where you cloned the repository.
    3. When asked, choose `Open as Project`.
    4. IntelliJ will parse Gradle buildscript `build.gradle.kts` file and configure the project accordingly.
6. Configure Gradle to build & run using IntelliJ IDEA:
    1. Go to `Preferences -> Build, Execution, Deployment -> Build Tools -> Gradle`.
    2. Configure `Build and run using` to IntellliJ IDEA.
    3. Configure `Run tests using` to IntellliJ IDEA.

   ![configure-intellij-gradle.png](/docs/resources/img/configure-intellij-gradle.png)

7. Refresh the project using Gradle's new configuration:
    1. Open Gradle tool window: `View -> Tool Windows -> Gradle`
    2. Click the `Reload All Gradle Projects` button.

   ![gradle-reload-all.png](/docs/resources/img/gradle-reload-all.png)

## Run Configurations

Several IntelliJ run configurations are provided to make it easier to run the project.

- `Docker - Run Dev Stack - Zookeeper, Kafka, DB`: runs the development stack locally, using `docker-compose`.
- `Run - All Servers - with Docker`: runs all the servers locally, using `docker-compose`.
- `Run - All Servers - no Docker`: runs all the servers locally, without running `docker-compose`.
- `Run - Sep Server - no Docker`: runs the SEP server locally, without running `docker-compose`.
- `Run - Platform Server - no Docker`: runs the Platform server locally, without running `docker-compose`.
- `Run - Event Processing Server - no Docker`: runs the Event Processing server locally, without
  running `docker-compose`.
- `Run - Custody Server - no Docker`: runs the Custody server locally, without running `docker-compose`.
- `Run - Java Reference Server - no Docker`: runs the Java Reference server locally, without running `docker-compose`.
- `Run - Kotlin Reference Server - no Docker`: runs the Kotlin Reference server locally, without
  running `docker-compose`.

The following run configurations are provided to run integration and end-2-end tests

- `Test - End2End Test - no fullstack`: runs the end-2-end tests locally, without running `docker-compose`.
- `Test - End2End Test - with fullstack`: runs the end-2-end tests locally, with running `docker-compose`.
- `Test - End2End with RPC Test - no fullstack`: runs the end-2-end tests with RPC locally, 
  without running `docker-compose`.
- `Test - End2End with RPC Test - with fullstack`: runs the end-2-end tests with RPC locally, 
  with running `docker-compose`.
- `Test - Fireblocks End2End Test - no fullstack`: runs the end-2-end tests with Fireblocks locally, 
  without running `docker-compose`.
- `Test - Fireblocks End2End Test - with fullstack`: runs the end-2-end tests with Fireblocks locally, 
  with running `docker-compose`. 
- `Test - Fireblocks End2End with RPC Test - no fullstack`: runs the end-2-end tests with Fireblocks and RPC locally,
  without running `docker-compose`.
- `Test - Fireblocks End2End with RPC Test - with fullstack`: runs the end-2-end tests with Fireblocks and RPC locally,
  with running `docker-compose`.
- `Test - Integration Test - no fullstack`: runs the integration tests locally, without running `docker-compose`.
- `Test - Integration Test - with fullstack`: runs the integration tests locally, with running `docker-compose`.

### Example: Debug the Platform Server

If you would like to debug the Platform server, you can do so by running the

- Make sure `docker` and `docker-compose` is available on your local machine.
- Run `Docker - Run Dev Stack - Zookeeper, Kafka, DB` to start the development stack.
- Run `Run - Sep Server - no Docker` to start the SEP server.
- Debug `Run - Platform Server - no Docker` to start debugging the Platform server.

### Example: Debug the Junit or the End-to-End Tests

If you would like to debug the unit tests or the end-to-end tests

- Make sure `docker` and `docker-compose` is available on your local machine.
- Run or Debug `Run - All Servers - with Docker`
- Debug `Test - End2End Test - no fullstack` or `Test - Integration Test - no fullstack`

## Running the Tests From Gradle in IntelliJ

To make sure your configuration worked, please make sure you can run all the tests successfully from the IDE:

1. Open the Gradle tool window: `View -> Tool Windows -> Gradle`.
2. Navigate to the all tests option: `Tasks -> verification -> test`.
3. Right-click it and select the `run` or `debug` option:
   ![running-the-tests.png](/docs/resources/img/running-the-tests.png)

__Note__: You may need to check there is no active docker containers running on your machine from the previous debug
sessions. If there are, please stop and delete them. 

