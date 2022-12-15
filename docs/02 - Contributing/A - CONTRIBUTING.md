# How to contribute

- [How to contribute](#how-to-contribute)
  - [Debeloper Tools](#debeloper-tools)
  - [How to Build with Gradle](#how-to-build-with-gradle)
    - [Clean](#clean)
    - [Build](#build)
    - [Running Unit Tests](#running-unit-tests)
  - [Coding Guidelines](#coding-guidelines)
    - [Coding Style](#coding-style)
    - [Git Commit Messages](#git-commit-messages)
    - [Pull Request Style](#pull-request-style)
    - [Logging Levels](#logging-levels)
  - [Database Migration](#database-migration)
  - [Publishing](#publishing)

👍🎉 First off, thanks for taking the time to contribute! 🎉👍

Check out the [Stellar Contribution Guide](https://github.com/stellar/.github/blob/master/CONTRIBUTING.md) that apply to all Stellar projects.

## Debeloper Tools

Check out the [Debeloper Tools](/docs/02%20-%20Contributing/B%20-%20Developer%20Tools.md).

## How to Build with Gradle

The projects can be built with Gradle version 7.3. If you have older gradle version installed, you may run `./gradlew`.

### Clean

`./gradlew clean`

### Build

Buile all projects: `./gradlew build`

Build a subproject: `./gradlew :[subproject]:build`

Build the Spring Boot application: `./gradlew bootJar`

### Running Unit Tests

Run all tests: `./gradlew test`

Run subproject tests: `./gradlew :[subproject]:test`

## Coding Guidelines

### Coding Style

This project uses [Google Java Format](https://github.com/google/google-java-format) to format the code, it's applied during the build process.

Also, despite the project being written in Java, the tests are written in Kotlin, so please make sure to follow this convention.

### Git Commit Messages

Write your commit message in the imperative: `"Fix bug"` and not `"Fixed bug"`
or `"Fixes bug."`  This convention matches up with commit messages generated
by commands like git merge and git revert. Example:

```text
Fix issue where Foo is handled as Bar.

Further paragraphs come after blank lines.

- Bullet points are okay, too
- Typically a hyphen or asterisk is used for the bullet, and bullets are wrapped by spaces before and after each bullet block.

If the commit relates to an issue, add a reference(s) to them at the bottom, like so:

Resolves: #123
```

### Pull Request Style

- When starting a pull request, it's recommended to start it as draft and only submit it when it's ready to be merged.
- Usually, it starts with a prefix that helps identifying what it's about or which modules it touches, like:
  - `docs: add documentation for Circle integration`
  - `SEP-31: add support for SEP-31 receiver`
  - `SEP-31+SEP-38: add support for SEP-31 receiver that accepts SEP-38 quotes`
- Make sure to fulfill the pull request `WHAT` and `WHY` sections.
- When merging the pull request, we use the squash & commit option.
- When merging the pull request, make sure to include the pull request WHAT & WHERE sections in the merge commit body, as well as the pull request title in the commit title. 

[`#300`](https://github.com/stellar/java-stellar-anchor-sdk/pull/300) is an example of a pull request that follows these guidelines, and [`29c7ef6`](https://github.com/stellar/java-stellar-anchor-sdk/commit/29c7ef66e94c0b9503ca68e15b07da064a76ee2d) is the commit where it got merged into the main branch.

### Logging Levels

When contributing code, think twice about a given log message because not every bit of information is equally important. 
Therefore, the logging levels should be used consistently.

Please follow the [Logging Guidelines](/docs/02%20-%20Contributing/C%20-%20Logging%20Guidelines.md).

## Database Migration

Please refer to [Database Migration](/docs/02%20-%20Contributing/D%20-%20Database%20Migration.md) for information on this topic.

## Publishing

Please refer to [Publishing the SDK](/docs/02%20-%20Contributing/E%20-%20Publishing%20the%20SDK.md) for information on this topic.

## IntelliJ Run Configurations

Please import the run configurations from the `.run` folder
- `1. Platform Server.run.xml` runs the platform server
- `2. Business Server.run.xml` runs the reference business server