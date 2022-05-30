provider "acme" {
  server_url = "https://acme-staging-v02.api.letsencrypt.org/directory"
  #server_url = "https://acme-v02.api.letsencrypt.org/directory"
}

data "aws_route53_zone" "anchor-zone" {
  name         = "${var.hosted_zone_name}"
  private_zone = false
}

resource "aws_route53_record" "sep" {
  zone_id = data.aws_route53_zone.anchor-zone.zone_id
  name    = "www.${data.aws_route53_zone.anchor-zone.name}"
  type    = "CNAME"
  ttl     = "300"
  records = [aws_lb.sep.dns_name]
}

resource "aws_route53_record" "ref" {
  zone_id = data.aws_route53_zone.anchor-zone.zone_id
  name    = "ref.${data.aws_route53_zone.anchor-zone.name}"
  type    = "CNAME"
  ttl     = "300"
  records = [aws_lb.ref.dns_name]
}

resource "tls_private_key" "private_key" {
  algorithm = "RSA"
}

resource "acme_registration" "registration" {
  account_key_pem = tls_private_key.private_key.private_key_pem
  email_address   = "reece@stellar.org" 
  depends_on = [aws_route53_record.sep]
}

resource "acme_certificate" "certificate" {
  account_key_pem           = acme_registration.registration.account_key_pem
  common_name               = data.aws_route53_zone.anchor-zone.name
  subject_alternative_names = ["*.${data.aws_route53_zone.anchor-zone.name}"]

  dns_challenge {
    provider = "route53"

    config = {
      AWS_HOSTED_ZONE_ID = data.aws_route53_zone.anchor-zone.zone_id
    }
  }

  depends_on = [aws_route53_record.sep, acme_registration.registration]
}

 data "aws_acm_certificate" "issued" {
  domain   = "www.${data.aws_route53_zone.anchor-zone.name}"
  statuses = ["ISSUED"]
  depends_on = [aws_route53_record.sep, acme_registration.registration, acme_certificate.certificate]
}

