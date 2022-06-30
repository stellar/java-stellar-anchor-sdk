resource "aws_ecs_service" "sep" {
 name                               = "${var.environment}-sep-service"
 cluster                            = aws_ecs_cluster.sep.id
 task_definition                    = aws_ecs_task_definition.sep.arn
 desired_count                      = 1
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