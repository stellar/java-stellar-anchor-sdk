variable "environment" {
  description = "deployment environment"
  type = string
  default = "dev"
}

variable "hosted_zone_name" {
  description = "name of hosted zone for anchor platform"
  type = string
}

variable "jwt_secret" {
  type  = string
  default = "secret"
}
variable "sep10_signing_seed" {
    type  = string
}

variable "sqlite_username" {
    type  = string
    default = "admin"
}
variable "sqlite_password" {
    type  = string
    default = "admin"
}

variable  "sqs_access_key" {
  type = string
}

variable  "sqs_secret_key" {
  type = string
}

variable "anchor_config_build_spec" {
  type = string
  default = "docs/resources/deployment-examples/aws-fargate-ecs/buildspec-dev.yml"
}

variable "anchor_config_repository" {
  type = string
  default = "https://github.com/reecexlm/java-stellar-anchor-sdk"
}

variable "aws_account" {
  type = string
  default = "245943599471"
}

variable "aws_region" {
  type = string
  default = "us-east-2"
}

variable "docker_user" {
  type = string
  default = "reecemarkowsky"
}

variable "docker_password_arn" {
  type = string
  default = "arn:aws:secretsmanager:us-east-2:245943599471:secret:/CodeBuild/dockerLoginPassword-83VCjq"
}