
variable "domain_name" {
  type    = string
  default = "arnovanoort.nl"
}

output "api_url_albums" {
  value = "${aws_apigatewayv2_stage.default.invoke_url}/albums"
}
