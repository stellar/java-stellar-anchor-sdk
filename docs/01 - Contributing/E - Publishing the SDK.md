# Publishing the SDK

- [Publishing the SDK](#publishing-the-sdk)
  - [Sign the Publications](#sign-the-publications)
    - [Create a GPG key pair](#create-a-gpg-key-pair)
    - [List GPG key pairs](#list-gpg-key-pairs)
    - [Export the private key](#export-the-private-key)
    - [Edit `~/.gradle/gradle.properties`](#edit-gradlegradleproperties)
  - [Publish to Nexus Repository](#publish-to-nexus-repository)
  - [Verify the Publication](#verify-the-publication)
  - [Import the Stellar Anchor Java SDK](#import-the-stellar-anchor-java-sdk)
    - [Maven](#maven)
    - [Gradle Groovy](#gradle-groovy)
    - [Gradle Kotlin](#gradle-kotlin)

This section covers how to publish the repository artifacts to Maven Central Repository, under the group id `stellar-anchor-sdk`.

## Sign the Publications
Before the publication can be published, it must be signed using a GPG key.

### Create a GPG key pair
If you don't have a GPG key, you need to generate the pair by running

```shell
> gpg2 --full-generate-key

Please select what kind of key you want:
   (1) RSA and RSA (default)
   (2) DSA and Elgamal
   (3) DSA (sign only)
   (4) RSA (sign only)
  (14) Existing key from card

Your Selection: 4
```

Select `4` to generate a sign-only RSA key pair and enter `4096` to create a 4096-bit RSA keys. Follow the prompt the finish.

### List GPG key pairs

To list all keys, run:

```shell
gpg2 --list-keys
```

To list all keys in short format, run:

```shell
gpg2 --list-keys --keyid-format short
```

### Export the private key

Now export the secret key in keyring format:

```shell
gpg --keyring secring.gpg --export-secret-keys > ~/.gnupg/secring.gpg
```

### Edit `~/.gradle/gradle.properties`

Now, we need to tell Gradle to use the new key for signing. In `~/.gradle/gradle.properties`, add/modify the 
following entries:

```shell
signing.keyId=${short-key-id}
signing.password=${key-password}
signing.secretKeyRingFile=${HOME_DIR}/.gnupg/secring.gpg
```

## Publish to Nexus Repository

Here are the steps to publishing the Stellar Anchor Java SDK to Maven Central:

1. Set the environment varibles: `OSSRH_USER=[OSSRH username]` and `OSSRH_PASSWORD=[OSSRH password]`. 
2. Run `./gradlew publish`. The `publish` task will sign all publications with the `sign` plug-in configured by `~/.gradle/gradle.properties`.
3. If `publish` task is executed successfully, we should see the repository successfully published. Open the [OSSRH Console](https://oss.sonatype.org) to manage the publication.
4. Under `Staging Repositories`, select the published repository.
5. Click on `Drop` to delete the publication, if that's what you want.
6. Click on `Close` to trigger the Maven Central validation process. The sync process of the artifactory to Maven Central is enabled by [OSSRH-78005](https://issues.sonatype.org/browse/OSSRH-78005) ticket.  

## Verify the Publication

The repository should be searchable at: [Maven Search](https://search.maven.org/search?q=org.stellar). It can take 2-4 hours after release before your artifacts show up in search results on search.maven.org

## Import the Stellar Anchor Java SDK

At this point, you can finally import the updated version of the SDK into your project.

### Maven
```
<dependency>
    <groupId>org.stellar.anchor-sdk</groupId>
    <artifactId>core</artifactId>
    <version>${version}</version>
</dependency>
```

### Gradle Groovy
`implementation group: 'org.stellar.anchor-sdk', name: 'core', version: '${version}'`

### Gradle Kotlin
`implementation("org.stellar.anchor-sdk:core:${version}")`
