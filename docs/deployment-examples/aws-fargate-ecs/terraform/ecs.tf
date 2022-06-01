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
  container_definitions = jsonencode([{
   name        = "${var.environment}-sep"
   image       = "reecemarkowsky/testing"
   entryPoint  = ["java", "-jar", "/app/anchor-platform-runner.jar", "--sep-server"]
   essential   = true
   logConfiguration = {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "firelens-container",
                    "awslogs-region": "us-east-2",
                    "awslogs-create-group": "true",
                    "awslogs-stream-prefix": "firelens"
                }
            }
   portMappings = [{
     protocol      = "tcp"
     containerPort = 8080
     hostPort      = 8080
   }]
  }])
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
   entryPoint  = ["java", "-jar", "/app/anchor-platform-runner.jar", "--anchor-reference-server"]
   logConfiguration = {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "firelens-container",
                    "awslogs-region": "us-east-2",
                    "awslogs-create-group": "true",
                    "awslogs-stream-prefix": "firelens"
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
   security_groups  = [aws_security_group.sep_alb.id]
   subnets          = module.vpc.public_subnets
   assign_public_ip = true
 }
 
 load_balancer {
   target_group_arn = aws_alb_target_group.sep.arn
   container_name   = "${var.environment}-sep"
   container_port   = 8080
 }
 
 lifecycle {
   ignore_changes = [task_definition, desired_count]
 }
 
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
   subnets          = module.vpc.public_subnets
   assign_public_ip = false
 }
 
 load_balancer {
   target_group_arn = aws_alb_target_group.ref.arn
   container_name   = "${var.environment}-ref"
   container_port   = 8081
 }
 
 lifecycle {
   ignore_changes = [task_definition, desired_count]
 }
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
   interval            = "300"
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
   interval            = "30"
   protocol            = "HTTP"
   matcher             = "200"
   timeout             = "3"
   path                = "/health"
   unhealthy_threshold = "2"
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
  certificate_arn   = aws_iam_server_certificate.sep_cert.arn
 
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
                "logs:CreateLogGroup"
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