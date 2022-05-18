data "aws_route53_zone" "anchor-zone" {
  name         = "${hosted_zone_name}"
  private_zone = false
}

data "kubernetes_service" "example" {
  metadata {
    name = "terraform-example"
  }
}

resource "aws_route53_record" "sep" {
  zone_id = data.aws_route53_zone.anchor-zone.zone_id
  name    = "sep.${data.aws_route53_zone.selected.name}"
  type    = "CNAME"
  ttl     = "300"
  records = [data.kubernetes_service.sep-service.status.0.load_balancer.0.ingress.0.hostname]
  depends_on = [
    helm_release.ingress-nginx
  ]
}

resource "aws_route53_record" "ref" {
  zone_id = data.aws_route53_zone.anchor-zone.zone_id
  name    = "ref.${data.aws_route53_zone.selected.name}"
  type    = "CNAME"
  ttl     = "300"
  records = [data.kubernetes_service.ref-service.status.0.load_balancer.0.ingress.0.hostname]
}