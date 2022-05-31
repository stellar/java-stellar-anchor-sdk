#provider "aws" {
#    region = "us-east-1"
#    alias = "regional"
#}

resource "local_file" "foo" {
    content  = "foo!"
    filename = "${path.module}/foo.bar"
}



locals {
  sepconfig = templatefile("${path.module}/templates/sep.tftpl",{
      "homeDomain"   = "www.stellaranchordemo.com"})
    appspec = templatefile("${path.module}/templates/appspec.tftpl",{})
}
  
data "archive_file" "deploy_zip" {
  type        = "zip"
  output_path = "${path.module}/generated_files/anchor-platform.zip"

  source {
    content  = local.sepconfig
    filename = "config/anchor-config.yaml"
  }

  source {
    content  = local.appspec
    filename = "config/appspec.yaml"
  }
}

resource "aws_s3_bucket_object" "file_upload" {
  #provider         = "aws.regional"
  bucket           = "sepconfig"
  key              = "anchorconfig.zip"
  source           = data.archive_file.deploy_zip.output_path
}


