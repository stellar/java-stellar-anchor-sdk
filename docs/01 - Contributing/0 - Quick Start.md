# How to quick start Anchor Platform
## Prepare Docker daemon
Before you start, please make sure that `docker` daemon or Docker Desktop is running.

## Git clone the repository
Download Anchor Platform repository from GitHub:
```shell
git clone git@github.com:stellar/java-stellar-anchor-sdk.git
cd java-stellar-anchor-sdk
```

## Run the Anchor Platform
### Version 2.x stable release
Run the Anchor Platform with version 2.x stable release in default configuration:
```shell
docker compose --profile v2-stable up
```
### Latest release
Run the Anchor Platform with latest release in default configuration:
```shell
docker compose --profile latest up
```

### Run the Anchor Platform with locally built image
Run the Anchor Platform with locally built image in default configuration:
```shell
docker build -t stellar/anchor-platform:local .
docker compose --profile local up
```