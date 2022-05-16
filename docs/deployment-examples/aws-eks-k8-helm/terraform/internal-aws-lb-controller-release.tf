resource "helm_release" "aws-lb-controller" {
  name             = "alb-ingress-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  namespace        = "awsloadbalancer"
  create_namespace = true
  wait             = true
  reset_values     = true
  max_history      = 3
  timeout          = 600
  
  values = [
    file("${path.module}/aws-lb-controller-values.yaml")
  ]

  set {
    name  = "clusterName"
    value = data.aws_eks_cluster.cluster.name
  }

  set {
    name  = "controller.admissionWebhooks.enabled"
    value = "false"
  }

  set { 
    name = "serviceAccount.name"
    value = "alb-ingress-controller"
  }
  
}

