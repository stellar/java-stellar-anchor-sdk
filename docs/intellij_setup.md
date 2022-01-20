# Setup IntelliJ

## Clone the repository
```
git clone git@github.com:stellar/java-stellar-anchor-dev.git
```

## Configure IntelliJ maven project
The `java-stellar-anchor-dev` is built with Maven. To configure the project in IntelliJ, do the following:

1. Launch IntelliJ
2. File -> Open `java-stellar-anchor-dev/pom.xml`
3. When asked, choose `Open as Project`. 

IntelliJ will parse Maven pom.xml file and configure the project according to the `pom.xml` file.

## Run Kotlin tests

This library is covered by unit tests written in Kotlin. To run/debug the tests, do the following:

1. Navigate to the `project` tab on the left.
2. Make sure you choose `Project` in the drop-down list of the project view.
3. Navigate to `src/test`
4. Right click on `kotlin` folder. Select `Run Tests in 'kotlin'.
5. If you want to debug, select `Debug Tests in 'kotlin'`. 

It will take IntelliJ sometime to download all dependencies, compile, and run. 

## Packaging

We use `maven package` to create the `.jar` file.

### Testing
```shell
mvn test
```
### Packaging

```shell
cd ./java-stellar-anchor-dev
mvn package
```
### Packaging without running tests (not recommended)
```shell
mvn package -DskipTests
```

