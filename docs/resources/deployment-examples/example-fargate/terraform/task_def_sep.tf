resource "aws_ecs_task_definition" "sep" {
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  family                   = "${var.environment}-sep"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn
  volume {
    name = "config"
  }
  
  container_definitions = jsonencode([{
   name        = "${var.environment}-sep-config"
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
                    "awslogs-region": "us-east-2",
                    "awslogs-create-group": "true",
                    "awslogs-stream-prefix": "sep"
                }
            }
  },{
   name        = "${var.environment}-sep"
   image       = "stellar/anchor-platform:latest"
   dependsOn =  [ {
     containerName = "${var.environment}-sep-config"
     condition = "START"
   }]
   #entryPoint = ["/anchor_config/sep.sh"]
   entryPoint  = ["java", "-jar", "/app/anchor-platform-runner.jar", "--sep-server"]
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
        "name": "SQLITE_USERNAME",
        "valueFrom": data.aws_ssm_parameter.sqlite_username.arn
      },
      {
        "name": "SQLITE_PASSWORD",
        "valueFrom": data.aws_ssm_parameter.sqlite_password.arn
      },
      {
        "name": "SEP_10_SIGNING_SEED",
        "valueFrom": data.aws_ssm_parameter.sep10_signing_seed.arn
      },
      {
        "name": "JWT_SECRET",
        "valueFrom": data.aws_ssm_parameter.jwt_secret.arn
      }
   ]

   "mountPoints": [
      {
        "readOnly": true,
        "containerPath": "/anchor_config",
        "sourceVolume": "config"
      }
    ]
    "environment": [
              {
                  "name": "STELLAR_ANCHOR_CONFIG",
                  "value": "file:/anchor_config/anchor_config.yaml"
              },
              {   
                  "name": "TEST3",
                  "value": "TEST3"
              }
          ],
      logConfiguration = {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "anchor-platform",
                    "awslogs-region": "us-east-2",
                    "awslogs-create-group": "true",
                    "awslogs-stream-prefix": "sep"
                }
            }
   portMappings = [{
     protocol      = "tcp"
     containerPort = 8080
     hostPort      = 8080
   }]
  }
  ])
}