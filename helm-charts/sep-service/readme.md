# Anchor Platform SEP Service Helm Chart
## Introduction
This chart installs the Stellar Anchor Platform SEP Serviceon Kubernetes cluster using the Helm package manager.
## Upgrading your Clusters
To upgrade the Strimzi operator, you can use the helm upgrade command.
## Installing the Chart
Add the Anchor Platform SEP Service Helm Chart repository:
```
$ helm repo add stellar https:/stellar.org/charts/
```
To install the chart with the release name my-release:
```
$ helm install my-release stellar/anchorplatform
```
## Uninstalling the Chart
To uninstall/delete the my-release deployment:

```
$ helm delete my-release
The command removes all the Kubernetes components associated with the operator and deletes the release.
```

## Database Access Configuration
Unless you are using sql-lite (default configuration) your values.yaml should contain database access configuration, it should contain both the stellar.anchor.data_access.type (currently only `data-spring-jdbc` is supported) and `stellar.anchor.data_access.setttings` which contains the name of the  yaml key (nested under key `stellar`) containing your database configuration settings.  For example, if you plan to use AWS Aurora, you would set the data_access type and settings along with the configuration for database access as follows:

```
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
## Basic Configuration
The following table lists the configurable parameters of the Anchor Platform chart and their default values.  These are also reflected in the example [values.yaml](./values.yaml).
|  Parameter | Description | Required?  | Default Value | 
|---|---|---|---|
|  fullName | customize anchor platform k8s resource names  eg < fullName >-configmap-< service.name >  |  yes  | anchor-platform  |
| service.containerPort | ingress backend container port   | yes  | 8080  |
| service.servicePort  |  ingress backend service port | yes  | 8080  |
| service.replicas  | number of instances of anchor platform  | yes  | 1 |
| service.type  | service type  | yes  | NodePort  |
| service.name  | name of service  | yes   | sep  |
| image.repo  | dockerhub image repository | yes  | stellar |
| image.name  | dockerhub image name  | yes  | anchor-platform  |
| image.tag  | dockerhub anchorplatform dag  | yes | latest |
| deployment.replicas  | number of instances  | yes  | 1 |
| deployment.envFrom | kubernetes secrets name | no  | n/a  |
| ingress.metadata  | ingress metadata (list)  | no  | n/a |
| ingress.labels  | ingress labels (list)  | no | n/a  |
| ingress.annotations | ingress annotations (list)  | no  | n/a  |
| ingress.tls.host  | tls certificate hostname | no  | n/a  |
| ingress.tls.secretName  | k8 secret holding tls certificate if reqd  | no  | n/a  |
| ingress.rules  | ingress backend rulues (list)  |   |   |

## Stellar Anchor Platform Configuration
The following table lists the additional configurable parameters of the Anchor Platform chart and their default values.
|  Parameter | Description | Required?  | Default Value | 
|---|---|---|---|
| stellar.anchor.data_access.type  | database access type  | yes  | data-spring-jdbc  |
| stellar.anchor.data_access.setttings  | values config root-level key for jdbc config  | yes | data-spring-jdbc-sqlite  |
| stellar.app_config.app.hostUrl  | URL of the Anchor Platform SEP Service   | y  | n/a |
| stellar.app_config.app.jwtSecretKey | web encryption key | ${JWT_SECRET_KEY} |
| sep1.enabled | sep1 true if service enabled | yes  | true  |
| sep1.stellarFile | location/name of stellar.toml file on service pod  | n | file:/config/stellar-wks.toml  |
| sep10.enabled | sep1 true if service enabled | yes | true |
| sep10.homeDomain |   |   |   |
| sep10.signingSeed |   |   |   |
| sep12.enabled  | sep1 true if service enabled | yes | true |
| sep12.customerIntegrationEndpoint| URL of SEP 12 KYC Endpoint  | no  | n/a  |
| sep31.enabled  | sep1 true if service enabled | yes | true |
| sep31.feeIntegrationEndPoint  | URL of Fees Endpoint  | no  | n/a  |
| sep38.enabled  | sep1 true if service enabled | yes | true |
| sep38.quoteIntegrationEndPoint  | URL of Quotes Endpoint  | no  | n/a  |
| event.enabled  | sep1 true if event service enabled | yes | true |
| event.publisherType  | kafka|sqs  | no  | kafka  |
| kafka_publisher.bootstrapServer  | kafka broker host:port | no | n/a  |
| kafka_publisher.useIAM  | use IAM Authentication for MSK  | no | true  |
