# How to publish `stellar-anchor-sdk`

## Sign the publications
Before the publication can be published, it must be signed.

### Create a GPG key pair
If you don't have a GPG key, you need to generate the pair by running
```
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
shell
### List GPG key pairsshell
shell
To list all keys, runshell
```shell
gpg2 --list-keys
```

To list all keys in short format, run 
```shell
gpg2 --list-keys --keyid-format short
```

### Export the private key
Now export the secret key in keyring format. Run
```shell
gpg --keyring secring.gpg --export-secret-keys > ~/.gnupg/secring.gpg
```

### Edit `~/.gradle/gradle.properties`
Now, we need to tell Gradle to use the key for `sign` plugin. In `~/.gradle/gradle.properties`, add/modify the 
following entries
```
signing.keyId=${short-key-id}
signing.password=${key-password}
signing.secretKeyRingFile=${HOME_DIR}/.gnupg/secring.gpg
```

## Publish to Nexus Repository
Stellar Anchor Java SDK publishes to MavenCentral. Here lists the steps to publish to MavenCentral.

1. Set environment varibles: `OSSRH_USER=[OSSRH username]` and `OSSRH_PASSWORD=[OSSRH password]`. 
2. Run `./gradlew publish`. The `publish` task will sign all publications with `sign` plug-in configured by `~/.gradle/gradle.properties`.
3. If `publish` task was executed successfully, we should see the repository successfully publishe.
   Open the [OSSRH Console](https://oss.sonatype.org) to manage the publication.

4. Under `Staging Repositories`, select the published repository.
5. Click on `Drop` to delete the publication.
6. Click on `Close` to trigger the MavenCentral validation process. The sync process of the artifactory to 
   MavenCentral is enabled by [OSSRH-78005](https://issues.sonatype.org/browse/OSSRH-78005) ticket.  

# Verify the Publication
The repository should be searchable at: [Maven Search](https://search.maven.org/search?q=org.stellar).

# Import the Stellar Anchor Java SDK

## Maven
```
<dependency>
    <groupId>org.stellar.anchor-sdk</groupId>
    <artifactId>core</artifactId>
    <version>${version}</version>
</dependency>
```

## Gradle Groovy
`implementation group: 'org.stellar.anchor-sdk', name: 'core', version: '${version}'`

## Gradle Kotlin
`implementation("org.stellar.anchor-sdk:core:${version}")`
