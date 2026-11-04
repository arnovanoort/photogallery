data "aws_route53_zone" "main" {
  name = "arnovanoort.nl"
}

resource "aws_route53_record" "root" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = ""
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.main.domain_name
    zone_id                = aws_cloudfront_distribution.main.hosted_zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "www" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "www"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.main.domain_name
    zone_id                = aws_cloudfront_distribution.main.hosted_zone_id
    evaluate_target_health = true
  }
}
# This will fetch the certificate from US-EAST
data "aws_acm_certificate" "cert" {
  provider    = aws.us_east_1     # Zoek in us-east-1 (gebruik de provider alias van hierboven!)
  domain      = var.domain_name
  statuses    = ["ISSUED"]        # Pak het goedgekeurde certificaat
  most_recent = true
}