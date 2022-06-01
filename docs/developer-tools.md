# JetBrains IntelliJ Setup

The project is mostly developed with IntelliJ.

## Steps to Setup IntelliJ Project 

## Setup IntelliJ

### Clone the repository
```
git clone git@github.com:stellar/java-stellar-anchor-sdk.git
```

### Configure IntelliJ Gradle project
The `java-stellar-anchor-sdk` is built with Gradle. To configure the project in IntelliJ, do the following:

1. Launch IntelliJ
2. File -> Open `java-stellar-anchor-sdk/build.gradle.kts`
3. When asked, choose `Open as Project`.

IntelliJ will parse Gradle buildscript `build.gradle.kts` file and configure the project accordingly.

### Run Kotlin tests

This library is covered by unit tests written in Kotlin. To run/debug the tests, do the following:

1. Navigate to the `project` tab on the left.
2. Make sure you choose `Project` in the drop-down list of the project view.
3. Navigate to `src/test`
4. Right click on `kotlin` folder. Select `Run Tests in 'kotlin'.
5. If you want to debug, select `Debug Tests in 'kotlin'`.

It will take IntelliJ sometime to download all dependencies, compile, and run.


## Packaging
```shell
./gradlew bootJar
```

### Testing
```shell
./gradlew test
```




