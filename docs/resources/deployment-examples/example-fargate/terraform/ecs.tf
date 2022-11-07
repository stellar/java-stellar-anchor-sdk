## ECS Cluster
resource "aws_ecs_cluster" "sep" {
  name = "sep-${var.environment}-cluster"
}
resource "aws_ecs_cluster" "ref" {
  name = "ref-${var.environment}-cluster"
}

## Task Definitions


resource "aws_iam_policy" "anchor_ssm_secrets" {
  name        = "${var.environment}-anchor_ssm_secrets_policy"
  path        = "/"
  description = "RO access to ssm"

  # Terraform's "jsonencode" function converts a
  # Terraform expression result to valid JSON syntax.
  policy = jsonencode(
    {
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ssm:Describe*",
                "ssm:Get*",
                "ssm:List*"
            ],
            "Resource": "*"
        }
    ]
}
  )
}

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "${var.environment}-anchorplatform-ecsTaskExecutionRole"
 
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


resource "aws_ecs_service" "ref" {
 name                               = "${var.environment}-ref-service"
 cluster                            = aws_ecs_cluster.ref.id
 task_definition                    = aws_ecs_task_definition.ref.arn
 desired_count                      = 1
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
  slow_start = 120
 
  health_check {
   healthy_threshold   = "2"
   interval            = "45"
   protocol            = "HTTP"
   matcher             = "200-299"
   timeout             = "20"
   path                = "/health"
   unhealthy_threshold = "5"
  }
  depends_on = [aws_lb.sep]
}

resource "aws_alb_target_group" "stellar_observer" {
  name        = "${var.environment}-stellar-observer-tg"
  port        = 8083
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"
  slow_start = 60
 
  health_check {
   healthy_threshold   = "2"
   interval            = "30"
   protocol            = "HTTP"
   matcher             = "200-299"
   timeout             = "20"
   path                = "/health"
   unhealthy_threshold = "5"
  }
  depends_on = [aws_lb.ref]
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
   matcher             = "200-299"
   timeout             = "10"
   path                = "/health"
   unhealthy_threshold = "5"
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

resource "aws_alb_listener" "stellar_observer" {
  load_balancer_arn = aws_lb.ref.arn
  port              = 8083
  protocol          = "HTTP"
 
  default_action {
   target_group_arn = aws_alb_target_group.stellar_observer.arn
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

resource "aws_iam_role_policy_attachment" "ecs-task-ssm-policy-attachment" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = aws_iam_policy.anchor_ssm_secrets.arn
}
