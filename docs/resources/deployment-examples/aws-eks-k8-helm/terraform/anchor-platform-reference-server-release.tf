locals {
  template_vars = {
    alb_ingress_iam_role_arn = aws_iam_role.eks_cluster.arn
    bootstrap_broker = "${element(split(",", data.aws_msk_cluster.anchor_msk.bootstrap_brokers), 0)}"
  }


  helm_chart_values = templatefile(
      "${path.module}/anchor-platform-reference-server-values.yaml",
      local.template_vars
  )
}


resource "helm_release" "reference" {
  name             = "reference-server"
  chart            = "./charts/reference"
  namespace        = "anchor-platform"
  version          = "17.1.3"
  create_namespace = true
  wait             = true
  reset_values     = true
  max_history      = 3
  timeout          = 600

values = [local.helm_chart_values]

}











