provider "aws" {
  region = var.aws_region
}

data "aws_availability_zones" "available" {}

resource "aws_eip" "nat" {
  count = 3
  vpc = true
}
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "3.2.0"

  name                 = "${var.environment}-anchorplatform-fargatedemo-vpc"
  cidr                 = "10.0.0.0/16"
  azs                  = data.aws_availability_zones.available.names
  private_subnets      = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets       = ["10.0.4.0/24", "10.0.5.0/24", "10.0.6.0/24"]
  enable_nat_gateway   = true
  single_nat_gateway   = false
  reuse_nat_ips        = true
  external_nat_ip_ids = "${aws_eip.nat.*.id}" 
  enable_dns_hostnames = true
}
