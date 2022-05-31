locals {
  sepconfig = templatefile("${path.module}/templates/sep.tftpl",
               {config = {
                   "homeDomain"   = "www.stellaranchordemo.com"
               }})
}
  

#Generate the Dockerrun.aws.json with the container information and environment tag
#resource "local_file" "docker_container_info" {
#  content  = local.file_content
#  filename = "./${var.service_name}-${var.environment}-Dockerrun.aws.json"
#}

#Zip the file
#data "archive_file" "source" {
#  type        = "zip"
#  source_dir  = "./${var.service_name}-${var.environment}-Dockerrun.aws.json"
#  output_path = "./${var.service_name}-${var.environment}-Dockerrun.aws.json.zip"

# depends_on = [
#    local_file.docker_container_info
#  ]
#}

/*
#Upload the zip file to s3 bucket under DockerRunFiles folder
resource "aws_s3_bucket_object" "file_upload" {
  bucket           = var.run_file_bucket
  key              = "${var.bucket_name}/${var.service_name}-${var.environment}-Dockerrun.aws.json.zip"
  source           = "${data.archive_file.source.output_path}"
}
*/


