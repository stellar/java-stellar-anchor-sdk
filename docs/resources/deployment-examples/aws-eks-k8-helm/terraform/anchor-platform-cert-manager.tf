
resource "helm_release" "cert-manager-crds" {
  name             = "cert-manager-crds"
  repository       = "https://charts.appscode.com/stable/"
  chart            = "cert-manager-crds"
  namespace        = "kube-system"
  wait             = true
  reset_values     = true
  max_history      = 3
  timeout          = 600
}


resource "helm_release" "cert-manager" {
  name             = "cert-manager"
  repository       = "https://charts.jetstack.io"
  chart            = "cert-manager"
  namespace        = "cert-manager"
  version          = "1.8.0"
  create_namespace = true
  wait             = true
  reset_values     = true
  max_history      = 3
  timeout          = 600
}

resource "helm_release" "cert-issuer" {
  name             = "cert-issuer"
  chart            = "./charts/cert-issuer"
  namespace        = "cert-manager"
  create_namespace = true
  wait             = true
  reset_values     = true
  max_history      = 3
  timeout          = 600
  depends_on = [resource.helm_release.cert-manager, resource.helm_release.reference]

      #values = []#"${path.module}/anchor-platform-sep-server-values.yaml"]
    values = [templatefile("${path.module}/anchor-platform-sep-server-values.yaml",
    local.sep_template_vars)]

}