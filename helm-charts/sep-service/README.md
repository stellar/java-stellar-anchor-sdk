# Anchor Platform SEP Service Helm Chart

## Introduction

This chart installs the Stellar Anchor Platform SEP Serviceon Kubernetes cluster using the Helm package manager.
## Upgrading your Clusters

To upgrade the Anchor Platform, you can use the helm upgrade command. example:
```sh
helm upgrade -f myvalues.yaml -f override.yaml --version 0.1.0 my-release stellar/anchorplatform
```
## Installing the Chart

Add the Anchor Platform SEP Service Helm Chart repository:
```sh
$ helm repo add stellar *stellar-helm-chart-repo* (this will be added once available)
```
To install the chart with the release name my-release:
```sh
$ helm install my-release stellar/anchorplatform
```
## Uninstalling the Chart

To uninstall/delete the my-release deployment:

```sh
$ helm delete my-release
The command removes all the Kubernetes components associated with the operator and deletes the release.
```

## Configuration

Helm Chart Configuration will be used to configure deployment via Helm Chart values substitution.  If a parameter value is not provided, Anchor Platform will attempt to use a `classpath` [default values file](../../platform/src/main/resources/anchor-config-defaults.yaml).  For further information on all available parameters and Anchor Platform configuration, please refer to the [Anchor Platform Configuration Guide](../../docs/00%20-%20Stellar%20Anchor%20Platform.md)

### Helm Chart Kubernetes Configuration

The following table lists the configurable parameters of the Anchor Platform chart and their default values.  These are also reflected in the example [values.yaml](./values.yaml).

| Parameter              | Description                                                                              | Required? | Default Value   | 
|------------------------|------------------------------------------------------------------------------------------|-----------|-----------------|
| fullName               | customize anchor platform k8s resource names  eg < fullName >-configmap-< service.name > | yes       | anchor-platform |
| service.containerPort  | ingress backend container port                                                           | yes       | 8080            |
| service.servicePort    | ingress backend service port                                                             | yes       | 8080            |
| service.replicas       | number of instances of anchor platform                                                   | yes       | 1               |
| service.type           | service type                                                                             | yes       | NodePort        |
| service.name           | name of service                                                                          | yes       | sep             |
| image.repo             | dockerhub image repository                                                               | yes       | stellar         |
| image.name             | dockerhub image name                                                                     | yes       | anchor-platform |
| image.tag              | dockerhub anchorplatform dag                                                             | yes       | latest          |
| deployment.replicas    | number of instances                                                                      | yes       | 1               |
| deployment.envFrom     | kubernetes secrets name                                                                  | no        | N/A             |
| ingress.metadata       | ingress metadata (list)                                                                  | no        | N/A             |
| ingress.labels         | ingress labels (list)                                                                    | no        | N/A             |
| ingress.annotations    | ingress annotations (list)                                                               | no        | N/A             |
| ingress.tls.host       | tls certificate hostname                                                                 | no        | N/A             |
| ingress.tls.secretName | k8 secret holding tls certificate if reqd                                                | no        | N/A             |
| ingress.rules          | ingress backend rules (list)                                                             |           |                 |

### Database

Unless you are using sql-lite (default configuration) your values.yaml should contain database access configuration, it should contain both the stellar.anchor.data_access.type (currently only `data-spring-jdbc` is supported) and `stellar.anchor.data_access.setttings` which contains the name of the  yaml key (nested under key `stellar`) containing your database configuration settings.  For example, if you plan to use AWS Aurora, you would set the data_access type and settings along with the configuration for database access as follows:

```yaml
stellar:
   anchor:
      data_access:
         type: data-spring-jdbc
         settings: data-spring-jdbc-aws-aurora-postgres

   data_spring_jdbc_aws_aurora_postgres:
      spring.jpa.generate-ddl: true
      spring.jpa.database: POSTGRESQL
      spring.jpa.show-sql: false
      spring.datasource.driver-class-name: org.postgresql.Driver
      spring.datasource.type: org.stellar.anchor.platform.databaseintegration.IAMAuthDataSource
      spring.datasource.url: jdbc:postgresql://database-aurora-iam-instance-1.chizvyczscs2.us-east-1.rds.amazonaws.com:5432/anchorplatform
      spring.datasource.username: anchorplatform1
      spring.datasource.hikari.max-lifetime: 840000    # 14 minutes because IAM tokens are valid for 15min
      spring.mvc.converters.preferred-json-mapper: gson
      spring.liquibase.change-log: classpath:/db/changelog/db.changelog-master.yaml
```

### Secrets Configuration

The following is an example kubernetes secrets manifest that will store base64 encoded secrets referenced using placeholders in the anchor platform configuration file.  In the following example, by replacing configuration values with ${JWT_SECRET} and ${SEP10_SIGNING_SEED} anchor platform will read those values from environment variables injected by the kubernetes deployment.

```yaml
apiVersion: v1
data:
  JWT_SECRET: c2VjcmV0
  SEP10_SIGNING_SEED: U0FYM0FINjIyUjJYVDZEWFdXU1JJRENNTVVDQ01BVEJaNVU2WEtKV0RPN00yRUpVQkZDM0FXNVg=
kind: Secret
metadata:
  name: apsigningseed
  namespace: sandbox
type: Opaque
```

# Assets Configuration

The following is an example  of the `assets` configuration that can be set in the helm chart's `values.yaml`.
Not setting this section will pass in an empty assets list.

```yaml
assets:
  - "schema": "stellar"
    "code": "USDC"
    "issuer": "GYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY"
    "distribution_account": "GZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"
    "significant_decimals": 2
    "deposit":
      "enabled": true
      "fee_minimum": 0
      "fee_percent": 0
      "min_amount": 0
      "max_amount": 10000
    "withdraw":
      "enabled": true
      "fee_fixed": 0
      "fee_percent": 0
      "min_amount": 0
      "max_amount": 10000
    "send":
      "fee_fixed": 0
      "fee_percent": 0
      "min_amount": 0
      "max_amount": 10000
    "sep31":
      "quotes_supported": true
      "quotes_required": true
      "sep12":
        "sender":
          "types":
            "sep31-sender":
              "description": "U.S. citizens limited to sending payments of less than $10,000 in value"
            "sep31-large-sender":
              "description": "U.S. citizens that do not have sending limits"
            "sep31-foreign-sender":
              "description": "non-U.S. citizens sending payments of less than $10,000 in value"
        "receiver":
          "types":
            "sep31-receiver":
              "description": "U.S. citizens receiving USD"
            "sep31-foreign-receiver":
              "description": "non-U.S. citizens receiving USD"
      "fields":
        "transaction":
          "receiver_routing_number":
            "description": "routing number of the destination bank account"
          "receiver_account_number":
            "description": "bank account number of the destination"
          "type":
            "description": "type of deposit to make"
            "choices":
              - "SEPA"
              - "SWIFT"
    "sep38":
      "exchangeable_assets":
        - "stellar:USDC:GXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
        - "iso4217:USD"
    "sep24_enabled": true
    "sep31_enabled": true
    "sep38_enabled": true
```

## Helm Chart Kubernetes Configuration

The following table lists the configurable parameters of the Anchor Platform chart and their default values.  These are also reflected in the example [values.yaml](./values.yaml).

| Parameter              | Description                                                                              | Required? | Default Value   |
|------------------------|------------------------------------------------------------------------------------------|-----------|-----------------|
| fullName               | customize anchor platform k8s resource names  eg < fullName >-configmap-< service.name > | yes       | anchor-platform |
| service.containerPort  | ingress backend container port                                                           | yes       | 8080            |
| service.servicePort    | ingress backend service port                                                             | yes       | 8080            |
| service.replicas       | number of instances of anchor platform                                                   | yes       | 1               |
| service.type           | service type                                                                             | yes       | NodePort        |
| service.name           | name of service                                                                          | yes       | sep             |
| image.repo             | dockerhub image repository                                                               | yes       | stellar         |
| image.name             | dockerhub image name                                                                     | yes       | anchor-platform |
| image.tag              | dockerhub anchorplatform dag                                                             | yes       | latest          |
| deployment.replicas    | number of instances                                                                      | yes       | 1               |
| deployment.envFrom     | kubernetes secrets name                                                                  | no        | n/a             |
| ingress.metadata       | ingress metadata (list)                                                                  | no        | n/a             |
| ingress.labels         | ingress labels (list)                                                                    | no        | n/a             |
| ingress.annotations    | ingress annotations (list)                                                               | no        | n/a             |
| ingress.tls.host       | tls certificate hostname                                                                 | no        | n/a             |
| ingress.tls.secretName | k8 secret holding tls certificate if reqd                                                | no        | n/a             |
| ingress.rules          | ingress backend rulues (list)                                                            |           |                 |

## Helm Chart Stellar Anchor Platform Configuration

The following table lists the additional configurable parameters of the Anchor Platform chart and their default values.

| Parameter                                                      | Description                                                                        | Required? | Default Value                |
|----------------------------------------------------------------|------------------------------------------------------------------------------------|-----------|------------------------------|
| stellar.app_config.app.hostUrl                                 | URL of the Anchor Platform SEP Service                                             | y         | N/A                          |
| stellar.app_config.app.jwtSecretKey                            | web encryption key                                                                 | y         | ${JWT_SECRET_KEY}            | N/A | 
| stellar.app_config.app.stellarNetwork                          | TESTNET OR PUBNET                                                                  | n         | TESTNET                      |
| stellar.app_config.app.logLevel                                | TRACE,DEBUG,INFO,WARN,ERROR,FATAL                                                  | n         | INFO                         |
| stellar.anchor.data_access.type                                | database access type                                                               | yes       | data-spring-jdbc             |
| stellar.anchor.data_access.setttings                           | values config root-level key for jdbc config                                       | yes       | data-spring-jdbc-sqlite      |
| stellar.toml.documentation.ORG_NAME                            | organization name to configure stellar.toml                                        | yes       | My Organization              |
| stellar.toml.documentation.ORG_URL                             | your organization URL to configure stellar.toml                                    | yes       | https:/myorg.org             |
| stellar.toml.documentation.ORG_DESCRIPTION                     | your organization description to configure stellar.toml                            | yes       | https://mylogo.png           |
| stellar.toml.documentation.ORG_SUPPORT_EMAIL                   | your organization support email address  to configure stellar.toml                 | yes       | myname@myorg.org             |
| stellar.app_config.app.integration_auth.auth_type              | Anchor Platform to Anchor Backend Authentication type JWT_TOKEN, API_KEY or NONE   | n         | NONE                         |
| stellar.app_config.app.integration_auth.platformToAnchorSecret | secret value                                                                       | n         | ${PLATFORM_TO_ANCHOR_SECRET} |
| stellar.app_config.app.integration_auth.anchorToPlatformSecret | secret value                                                                       | n         | ${ANCHOR_TO_PLATFORM_SECRET} |
| stellar.app_config.app.integration_auth.expirationMilliseconds | integration auth credential expiration ms                                          | n         | 30000                        |
| stellar.app_config.app.anchor_callback                         | endpoint to retrieve unique address & memo for sep 31 post /transaction            | n         | N/A                          |
| sep1.enabled                                                   | sep1 true if service enabled                                                       | yes       | true                         |
| sep10.enabled                                                  | sep1 true if service enabled                                                       | yes       | true                         |
| sep10.homeDomain                                               | a domain hosting a SEP-1 stellar.toml                                              | n         | N/A                          |
| sep10.signingSeed                                              | secret key of Stellar Account used for auth. (public key in stellar.toml ACCOUNTS) | y         | N/A                          |
| sep12.enabled                                                  | sep1 true if service enabled                                                       | yes       | true                         |
| sep12.customerIntegrationEndpoint                              | URL of SEP 12 KYC Endpoint                                                         | no        | N/A                          |
| sep31.enabled                                                  | sep1 true if service enabled                                                       | yes       | true                         |
| sep31.feeIntegrationEndPoint                                   | URL of Fees Endpoint                                                               | no        | N/A                          |
| sep38.enabled                                                  | sep1 true if service enabled                                                       | yes       | true                         |
| sep38.quoteIntegrationEndPoint                                 | URL of Quotes Endpoint                                                             | no        | N/A                          |
| event.enabled                                                  | sep1 true if event service enabled                                                 | yes       | true                         |
| event.publisherType                                            | kafka                                                                              | sqs       | no                           | kafka  |
| kafka_publisher.bootstrapServer                                | kafka broker host:port                                                             | no        | N/A                          |
| kafka_publisher.useIAM                                         | use IAM Authentication for MSK                                                     | no        | true                         |
| assets                                                         | see [Assets Configuration](#assets-configuration)                                  | yes       | []                           |
