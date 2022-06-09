# Developer Tools

- [Developer Tools](#developer-tools)
  - [Configuring Gradle on IntelliJ IDEA](#configuring-gradle-on-intellij-idea)
  - [Running the Tests on IntelliJ IDEA](#running-the-tests-on-intellij-idea)
  - [CLI Commands](#cli-commands)

The project is mostly developed with IntelliJ, therefore we will only cover the project configuration for that IDE.

## Configuring Gradle on IntelliJ IDEA

1. Clone the repository:

    ```shell
    git clone git@github.com:stellar/java-stellar-anchor-sdk.git
    ```

2. Configure IntelliJ as a Gradle project, since the `java-stellar-anchor-sdk` is built with Gradle:

   1. Launch IntelliJ
   2. File -> Open `java-stellar-anchor-sdk/build.gradle.kts`
   3. When asked, choose `Open as Project`.
   4. IntelliJ will parse Gradle buildscript `build.gradle.kts` file and configure the project accordingly.

3. Configure Gradle to build & run using IntelliJ IDEA:
   1. Go to `Preferences -> Build, Execution, Deployment -> Build Tools -> Gradle`.
   2. Configure `Build and run using` to IntellliJ IDEA.
   3. Configure `Run tests using` to IntellliJ IDEA.
   
   ![configure-intellij-gradle.png](img/configure-intellij-gradle.png)

4. Refresh the project using Gradle's new configuration:
   1. Open Gradle tool window: `View -> Tool Windows -> Gradle`
   2. Click the `Reload All Gradle Projects` button.

   ![gradle-reload-all.png](img/gradle-reload-all.png)

## Running the Tests on IntelliJ IDEA

To make sure your configuration worked, please make sure you can run all the tests successfully from the IDE:

1. Open the Gradle tool window: `View -> Tool Windows -> Gradle`.
2. Navigate to the all tests option: `Tasks -> verification -> test`.
3. Right-click it and select the run option:

    > Note i: when you right click the tests, you also have the option to debug it, which allows to add breakpoints and such.

    > Note ii: this step includes downloading & installing the dependencies, building the project and running it.

    ![running-the-tests.png](img/running-the-tests.png)

## CLI Commands

To build & package the project from the command line, run:

```shell
./gradlew bootJar
```

To test the project from command line, run:

```shell
./gradlew test
```
