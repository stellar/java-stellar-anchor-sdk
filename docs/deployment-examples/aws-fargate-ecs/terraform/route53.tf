provider "acme" {
  server_url = "https://acme-staging-v02.api.letsencrypt.org/directory"
#  server_url = "https://acme-v02.api.letsencrypt.org/directory"
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

#resource "tls_private_key" "private_key" {
#  algorithm = "RSA"
#}

#resource "acme_registration" "registration" {
#  account_key_pem = tls_private_key.private_key.private_key_pem
#  email_address   = "reece@stellar.org" 
#  depends_on = [aws_route53_record.sep, tls_private_key.private_key]
#}

#resource "acme_certificate" "certificate" {
#  account_key_pem           = acme_registration.registration.account_key_pem
#  common_name               = "www.${data.aws_route53_zone.anchor-zone.name}"
  #subject_alternative_names = ["*.${data.aws_route53_zone.anchor-zone.name}"]

#  dns_challenge {
#    provider = "route53"

 #   config = {
 ##     AWS_HOSTED_ZONE_ID = data.aws_route53_zone.anchor-zone.zone_id
  #  }
 # }

  #depends_on = [aws_route53_record.sep, acme_registration.registration]
#}

resource "aws_acm_certificate" "acm_certificate" {
  domain_name               = "www.${data.aws_route53_zone.anchor-zone.name}"
  validation_method         = "DNS"
lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate_validation" "acm_certificate_validation" {
 certificate_arn = aws_acm_certificate.acm_certificate.arn
 validation_record_fqdns = [ aws_route53_record.sep.fqdn]
}




#resource "aws_iam_server_certificate" "sep_cert" {
#  name             = "sepcert"
#  certificate_body = acme_certificate.certificate.certificate_pem
#  private_key      = acme_certificate.certificate.private_key_pem
#}

