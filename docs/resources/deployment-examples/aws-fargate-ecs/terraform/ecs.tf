## ECS Cluster
resource "aws_ecs_cluster" "sep" {
  name = "sep-${var.environment}-cluster"
}
resource "aws_ecs_cluster" "ref" {
  name = "ref-${var.environment}-cluster"
}

## Task Definitions
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
   image       = "245943599471.dkr.ecr.us-east-2.amazonaws.com/anchor-platform-config:latest"
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
   image       = "245943599471.dkr.ecr.us-east-2.amazonaws.com/anchorplatform:latest"
   dependsOn =  [ {
     containerName = "${var.environment}-sep-config"
     condition = "START"
   }]
   entryPoint = ["/anchor_config/sep.sh"]
   #entryPoint  = ["java", "-jar", "/app/anchor-platform-runner.jar", "--sep-server"]
   essential   = true

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
resource "aws_ecs_task_definition" "ref" {
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
    family                   = "${var.environment}-ref"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn
  container_definitions = jsonencode([{
   name        = "${var.environment}-ref"
   image       = "reecemarkowsky/testing"
   entryPoint = ["/config/sep.sh"]
   #entryPoint  = ["sh", "-c", "/bin/sh -c \"ls -l /config; java -jar /app/anchor-platform-runner.jar --anchor-reference-server\""]
   #"java", "-jar", "/app/anchor-platform-runner.jar", "--anchor-reference-server"]
    #"/bin/sh -c \"echo '<html> <head> <title>Amazon ECS Sample App</title> <style>body {margin-top: 40px; background-color: #333;} </style> </head><body> <div style=color:white;text-align:center> <h1>Amazon ECS Sample App</h1> <h2>Congratulations!</h2> <p>Your application is now running on a container in Amazon ECS.</p> </div></body></html>' >  /usr/local/apache2/htdocs/index.html && httpd-foreground\""
   logConfiguration = {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "anchor-platform",
                    "awslogs-region": "us-east-2",
                    "awslogs-create-group": "true",
                    "awslogs-stream-prefix": "ref"
                }
            }
   essential   = true
   #environment = [
   #  { "name" : "STELLAR_ANCHOR_CONFIG", "value" : "/config" },
   #  { "name" : "string", "value" : "string" }
   #]
   portMappings = [{
     protocol      = "tcp"
     containerPort = 8081
     hostPort      = 8081
   }]
  }])
}

resource "aws_iam_role" "ecs_task_role" {
  name = "anchorplatform-ecsTaskRole"
 
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

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "anchorplatform-ecsTaskExecutionRole"
 
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
 
resource "aws_iam_role_policy_attachment" "ecs-task-execution-role-policy-attachment" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

####################### ECS Service
resource "aws_ecs_service" "sep" {
 name                               = "${var.environment}-sep-service"
 cluster                            = aws_ecs_cluster.sep.id
 task_definition                    = aws_ecs_task_definition.sep.arn
 desired_count                      = 2
 deployment_minimum_healthy_percent = 100
 deployment_maximum_percent         = 200
 launch_type                        = "FARGATE"
 scheduling_strategy                = "REPLICA"
 
 network_configuration {
   security_groups  = [aws_security_group.sep.id]
   subnets          = module.vpc.private_subnets
   assign_public_ip = false
 }
 
 load_balancer {
   target_group_arn = aws_alb_target_group.sep.arn
   container_name   = "${var.environment}-sep"
   container_port   = 8080
 }
 
 #lifecycle {
 #  ignore_changes = [task_definition, desired_count]
 #}
 
}

resource "aws_ecs_service" "ref" {
 name                               = "${var.environment}-ref-service"
 cluster                            = aws_ecs_cluster.ref.id
 task_definition                    = aws_ecs_task_definition.ref.arn
 desired_count                      = 2
 deployment_minimum_healthy_percent = 100
 deployment_maximum_percent         = 200
 launch_type                        = "FARGATE"
 scheduling_strategy                = "REPLICA"
 
 network_configuration {
   security_groups  = [aws_security_group.ref_alb.id]
   subnets          = module.vpc.private_subnets
   assign_public_ip = false
 }
 
 load_balancer {
   target_group_arn = aws_alb_target_group.ref.arn
   container_name   = "${var.environment}-ref"
   container_port   = 8081
 }
 
 #lifecycle {
 #  ignore_changes = [task_definition, desired_count]
 #}
  depends_on = [aws_alb_listener.sep_http]
}

####################### ALB 
resource "aws_lb" "sep" {
  name               = "sep-${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.sep_alb.id]
  subnets            = module.vpc.public_subnets
 
  enable_deletion_protection = false
}
resource "aws_lb" "ref" {
  name               = "ref-${var.environment}-alb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.ref_alb.id]
  subnets            = module.vpc.private_subnets
 
  enable_deletion_protection = false
}


####################### Target Group
 
resource "aws_alb_target_group" "sep" {
  name        = "${var.environment}-sep-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"
 
  health_check {
   healthy_threshold   = "2"
   interval            = "30"
   protocol            = "HTTP"
   matcher             = "200"
   timeout             = "3"
   path                = "/.well-known/stellar.toml"
   unhealthy_threshold = "3"
  }
  depends_on = [aws_lb.sep]
}


resource "aws_alb_target_group" "ref" {
  name        = "${var.environment}-ref-tg"
  port        = 8081
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"
 
  health_check {
   healthy_threshold   = "3"
   interval            = "15"
   protocol            = "HTTP"
   matcher             = "200"
   timeout             = "3"
   path                = "/health"
   unhealthy_threshold = "10"
  }
  depends_on = [aws_lb.sep]
}

####################### LISTENER

resource "aws_alb_listener" "sep_http" {
  load_balancer_arn = aws_lb.sep.arn
  port              = 80
  protocol          = "HTTP"
 
  default_action {
   type = "redirect"
 
   redirect {
     port        = 443
     protocol    = "HTTPS"
     status_code = "HTTP_301"
   }
  }
  depends_on = [aws_lb.sep]
}

resource "aws_alb_listener" "sep_https" {
  load_balancer_arn = aws_lb.sep.arn
  port              = 443
  protocol          = "HTTPS"
 
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = aws_acm_certificate.sep.arn
 
  default_action {
    target_group_arn = aws_alb_target_group.sep.arn
    type             = "forward"
  }
  depends_on = [aws_lb.sep]
}

resource "aws_alb_listener" "ref_http" {
  load_balancer_arn = aws_lb.ref.arn
  port              = 8081
  protocol          = "HTTP"
 
  default_action {
   target_group_arn = aws_alb_target_group.ref.arn
   type             = "forward" 
  }
  depends_on = [aws_lb.ref]
}
 

resource "aws_iam_policy" "create_log_group" {
  name        = "createloggroups"
  description = "Create Log Group"
 
  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "sqs:*"
            ],
            "Resource": "*"
        }
    ]
}
EOF
}


 
resource "aws_iam_role_policy_attachment" "ecs-task-role-policy-attachment" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = aws_iam_policy.create_log_group.arn
}
