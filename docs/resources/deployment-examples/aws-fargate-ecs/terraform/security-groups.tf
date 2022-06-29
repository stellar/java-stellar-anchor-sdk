resource "aws_security_group" "sep_alb" {
  name   = "sep-${var.environment}-sg-alb"
  vpc_id = module.vpc.vpc_id
 
  ingress {
   protocol         = "tcp"
   from_port        = 80
   to_port          = 80
   cidr_blocks      = ["0.0.0.0/0"]
   ipv6_cidr_blocks = ["::/0"]
  }
 
  ingress {
   protocol         = "tcp"
   from_port        = 443
   to_port          = 443
   cidr_blocks      = ["0.0.0.0/0"]
   ipv6_cidr_blocks = ["::/0"]
  }
 
  egress {
   protocol         = "-1"
   from_port        = 0
   to_port          = 0
   cidr_blocks      = ["0.0.0.0/0"]
   ipv6_cidr_blocks = ["::/0"]
  }
}

resource "aws_security_group" "sep" {
  name   = "ref-${var.environment}-sg-sep"
  vpc_id = module.vpc.vpc_id
 
  ingress {
   protocol         = "tcp"
   from_port        = 8080
   to_port          = 8080
   cidr_blocks      = ["0.0.0.0/0"]
   ipv6_cidr_blocks = ["::/0"]
  }
 
  egress {
   protocol         = "-1"
   from_port        = 0
   to_port          = 0
   cidr_blocks      = ["0.0.0.0/0"]
   ipv6_cidr_blocks = ["::/0"]
  }
}

resource "aws_security_group" "ref_alb" {
  name   = "ref-${var.environment}-sg-alb"
  vpc_id = module.vpc.vpc_id
 
  ingress {
   protocol         = "tcp"
   from_port        = 8081
   to_port          = 8081
   cidr_blocks      = ["0.0.0.0/0"]
   ipv6_cidr_blocks = ["::/0"]
  }
 
  egress {
   protocol         = "-1"
   from_port        = 0
   to_port          = 0
   cidr_blocks      = ["0.0.0.0/0"]
   ipv6_cidr_blocks = ["::/0"]
  }
}

#resource "aws_security_group" "ecs_tasks" {
#  name   = "${var.environment}-anchorplatform-sg-task"
#  vpc_id = module.vpc.vpc_id
 
#  ingress {
#   protocol         = "tcp"
#   from_port        = var.container_port
#   to_port          = var.container_port
#   cidr_blocks      = ["0.0.0.0/0"]
##   ipv6_cidr_blocks = ["::/0"]
 # }
 
#  egress {
#   protocol         = "-1"
#   from_port        = 0
#   to_port          = 0
#   cidr_blocks      = ["0.0.0.0/0"]
#   ipv6_cidr_blocks = ["::/0"]
#  }
#}

