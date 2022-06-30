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
