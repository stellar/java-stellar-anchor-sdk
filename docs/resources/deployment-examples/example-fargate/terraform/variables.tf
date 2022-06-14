variable "environment" {
  description = "deployment environment"
  type = string
  default = "dev"
}

variable "hosted_zone_name" {
  description = "name of hosted zone for anchor platform"
  type = string
  #default = "stellaranchordemo.com"
}