resource "aws_ecs_task_definition" "ref" {
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
    family                   = "${var.environment}-ref"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

   volume {
    name = "config"
  }

  container_definitions = jsonencode([{
   name        = "${var.environment}-ref-config"
   image       = "${var.aws_account}.dkr.ecr.${var.aws_region}.amazonaws.com/${aws_ecr_repository.anchor_config.name}:latest"
   entryPoint  = ["/copy_config.sh"]
   
   essential   = false
   "mountPoints": [
      {
        "readOnly": false,
        "containerPath": "/anchor_config",
        "sourceVolume": "config"
      }
    ],    
   logConfiguration = {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "anchor-platform",
                    "awslogs-region": "${var.aws_region}",
                    "awslogs-create-group": "true",
                    "awslogs-stream-prefix": "ref-config"
                }
            }
  },
  {
   name        = "${var.environment}-ref"
   image       = "stellar/anchor-platform:${var.image_tag}"
   dependsOn =  [ {
     containerName = "${var.environment}-ref-config"
     condition = "START"
   }]
   entryPoint  = ["java", "-jar", "/app/anchor-platform-runner.jar", "--anchor-reference-server"]
   "mountPoints": [
      {
        "readOnly": true,
        "containerPath": "/anchor_config",
        "sourceVolume": "config"
      }
    ]
    "environment": [
              {
                  "name": "REFERENCE_SERVER_CONFIG_ENV",
                  "value": "file:/anchor_config/reference_config.yaml"
              }
          ],
    logConfiguration = {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "anchor-platform",
                    "awslogs-region": "${var.aws_region}",
                    "awslogs-create-group": "true",
                    "awslogs-stream-prefix": "ref"
                }
            }
   essential   = true
   secrets = [
      {
        "name": "SQS_ACCESS_KEY",
        "valueFrom": data.aws_ssm_parameter.sqs_access_key.arn
      },
      {
        "name": "SQS_SECRET_KEY",
        "valueFrom": data.aws_ssm_parameter.sqs_secret_key.arn
      },
       {
        "name": "PLATFORM_TO_ANCHOR_SECRET",
        "valueFrom": data.aws_ssm_parameter.platform_to_anchor_secret.arn
      },
      {
        "name": "ANCHOR_TO_PLATFORM_SECRET",
        "valueFrom": data.aws_ssm_parameter.anchor_to_platform_secret.arn
      },
      {
        "name": "JWT_SECRET",
        "valueFrom": data.aws_ssm_parameter.jwt_secret.arn
      }
   ]
   portMappings = [{
     protocol      = "tcp"
     containerPort = 8081
     hostPort      = 8081
   }]
  }])
}

resource "aws_iam_role" "ecs_task_role" {
  name = "${var.environment}-anchor-platform-ecsTaskRole"
 
  assume_role_policy = <<EOF
{
 "Version": "2012-10-17",
 "Statement": [
   {
     "Action": "sts:AssumeRole",
     "Principal": {
       "Service": "ecs-tasks.amazonaws.com"
     },
     "Effect": "Allow",
     "Sid": ""
   }
 ]
}
EOF
}
