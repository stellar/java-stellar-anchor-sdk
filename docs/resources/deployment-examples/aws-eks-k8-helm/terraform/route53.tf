data "aws_route53_zone" "anchor-zone" {
  name         = "${var.hosted_zone_name}"
  private_zone = false
}

data "kubernetes_ingress" "sep_ingress" {
  metadata {
    namespace = "anchor-platform"
    name = "sep-server-ingress"
  }
  depends_on = [
    resource.helm_release.sep
  ]
}

data "kubernetes_ingress" "ref_ingress" {
  metadata {
    namespace = "anchor-platform"
    name = "reference-server-ingress"
  }
  depends_on = [
      resource.helm_release.reference
  ]
}
resource "aws_route53_record" "sep" {
  zone_id = data.aws_route53_zone.anchor-zone.zone_id
  name    = "www.${data.aws_route53_zone.anchor-zone.name}"
  type    = "CNAME"
  ttl     = "300"
  records = [data.kubernetes_ingress.sep_ingress.status.0.load_balancer.0.ingress.0.hostname]
  depends_on = [
    helm_release.ingress-nginx
  ]
}

resource "aws_route53_record" "ref" {
  zone_id = data.aws_route53_zone.anchor-zone.zone_id
  name    = "ref.${data.aws_route53_zone.anchor-zone.name}"
  type    = "CNAME"
  ttl     = "300"
  #records = [data.kubernetes_service.ref_service.status.0.load_balancer.0.ingress.0.hostname]
  records = [data.kubernetes_ingress.ref_ingress.status.0.load_balancer.0.ingress.0.hostname]

}