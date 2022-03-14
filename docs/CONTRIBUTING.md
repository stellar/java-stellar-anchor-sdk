# How to contribute

👍🎉 First off, thanks for taking the time to contribute! 🎉👍

Check out the [Stellar Contribution Guide](https://github.com/stellar/.github/blob/master/CONTRIBUTING.md) that apply to all Stellar projects.

## Coding Style Guides
Check out the [Git Commit and Coding Style Guide](./stellar_anchor_platform_coding_style.md).

## Development Tools Helps
Check out the [Development Tools Help](./developer_tools.md).

# Subprojects
[Core](core.md): The SDK core library.

[Payment Services](payment-services.md): The payment service library.

[Anchor Platform](anchor-platform.md): The anchor platform Spring Boot application.

# Publishing 
[How to publish](publishing.md): How to publish `org.stellar.anchor-sdk:core:${version}`.

# How to Build with Gradle
The projects can be built with Gradle version 7.3. If you have older gradle version installed, you may run `./gradlew`.

## Build
Buile all projects: `./gradlew build`

Build a subproject: `./gradlew :[subproject]:build`

Build the Spring Boot application: `./gradlew bootJar`

## Running unit tests
Run all tests: `./gradlew test`

Run subproject tests: `./gradlew :[subproject]:test`

## Clean
`./gradlew clean`

