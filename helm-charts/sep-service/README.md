# Anchor Platform SEP Service Helm Chart

## Introduction

This chart installs the Stellar Anchor Platform SEP Serviceon Kubernetes cluster using the Helm package manager.
## Upgrading your Clusters

To upgrade the Anchor Platform, you can use the helm upgrade command. example:
```
helm upgrade -f myvalues.yaml -f override.yaml --version 0.4.0 my-release stellar/anchorplatform
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
The following table lists the configurable parameters related to k8s configuration of the helm chart and their default values.
These are also reflected in the example [values.yaml](./values.yaml).

| Parameter               | Description                                                                              | Required?  | Default Value   | 
|-------------------------|------------------------------------------------------------------------------------------|---|-----------------|
| fullName                | customize anchor platform k8s resource names  eg < fullName >-configmap-< service.name > |  yes  | anchor-platform |
| service.containerPort   | ingress backend container port                                                           | yes  | 8080            |
| service.servicePort     | ingress backend service port                                                             | yes  | 8080            |
| service.replicas        | number of instances of anchor platform                                                   | yes  | 1               |
| service.type            | service type                                                                             | yes  | NodePort        |
| service.name            | name of service                                                                          | yes   | sep             |
| image.repo              | dockerhub image repository                                                               | yes  | stellar         |
| image.name              | dockerhub image name                                                                     | yes  | anchor-platform |
| image.tag               | dockerhub anchorplatform tag                                                             | yes | latest          |
| image.pullPolicy        | image pull policy                                                                        | yes | always          |
| deployment.annotations  | deployment annotations                                                                   | yes  | 1               |
| deployment.envVars      | k8s env vars (`env`, `envFrom`, `configMapRef`)                                          | no  | N/A             |
| deployment.volumeMounts | additional k8s volumes                                                                   | no  | N/A             |
| ingress.metadata        | ingress metadata (list)                                                                  | no  | N/A             |
| ingress.labels          | ingress labels (list)                                                                    | no | N/A             |
| ingress.annotations     | ingress annotations (list)                                                               | no  | N/A             |
| ingress.tls.host        | tls certificate hostname                                                                 | no  | N/A             |
| ingress.tls.secretName  | k8s secret holding tls certificate if reqd                                               | no  | N/A             |
| ingress.rules           | ingress backend rules (list)                                                             |   |                 |

### Secrets Configuration
The following is an example kubernetes secrets manifest that will store base64 encoded secrets referenced using placeholders in the Anchor Platform configuration file.
In the following example, Anchor Platform will read the specified values from environment variables injected by the kubernetes deployment.

```yaml
apiVersion: v1
data:
  SECRET.SEP10.JWT_SECRET: c2VjcmV0
  SECRET.SEP10.SIGNING_SEED: U0FYM0FINjIyUjJYVDZEWFdXU1JJRENNTVVDQ01BVEJaNVU2WEtKV0RPN00yRUpVQkZDM0FXNVg=
  SECRET.CALLBACK_API.AUTH_SECRET: UExBVEZPUk1fVE9fQU5DSE9SX1NFQ1JFVAo=
  SECRET.PLATFORM_API.AUTH_SECRET: QU5DSE9SX1RPX1BMQVRGT1JNX1NFQ1JFVAo=
kind: Secret
metadata:
  name: apsigningseed
  namespace: sandbox
type: Opaque
```

## Helm Chart Stellar Anchor Platform Configuration
The following table lists additional configurable parameters for the Anchor Platform application 
and their default values. For a full list of configuration options, see Anchor Platform's configuration documentation.

| Parameter                                              | Description                                                                      | Default Value         |
|--------------------------------------------------------|----------------------------------------------------------------------------------|-----------------------|
| assets_config                                          | inline definition of assets file                                |                       |
| toml_config                                            | inline defintion of SEP1 stellar toml file                                       |                       |
| app_config.host_url                                    | URL of the Anchor Platform SEP Service                                           | http://localhost:8080 |
| app_config.data.type                                   | database type: `h2` (in-memory), `sqlite`, `postgres`, `aurora` | h2                    |
| app_config.callback_api.base_url                                  | callback endpoint         | localhost:8081        |
| app_config.callback_api.auth.type                                 | Anchor Platform to Anchor Backend Authentication type JWT_TOKEN, API_KEY or NONE | NONE                  |
| app_config.logging.level                            | TRACE,DEBUG,INFO,WARN,ERROR,FATAL                                                | INFO                  |
| app_config.sep1.enabled                                | whether sep1 service is enabled                                                     | false                 |
| app_config.sep10.enabled                               | whether sep10 service is enabled                                                    | false                 |
| app_config.sep12.enabled                               | whether sep12 service is enabled                                                 | false                 |
| app_config.sep31.enabled                               | whether sep31 service is enabled                                                    | false                 |
| app_config.sep38.enabled                               | whehter sep38 service is enabled                                                    | false                 |
| app_config.events.enabled                                        | whether event service is enabled                                               | false                 |
| app_config.events.publisher_type                                         | message broker type: kafka or sqs  | kafka                 |
| app_config.events.options.bootstrapServer              | event broker host:port                                                           | localhost:29092       |

