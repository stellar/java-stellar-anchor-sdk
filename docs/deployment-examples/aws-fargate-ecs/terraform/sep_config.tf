locals {
  sepconfig = templatefile("${path.module}/templates/sep.tftpl",
               {config = {
                   "homeDomain"   = "www.stellaranchordemo.com"
                }
               }
               )
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


resource "aws_s3_bucket_object" "file_upload" {
  bucket           = "testbucket"
  key              = "sepconfig"
  source           = local.sepconfig
}


