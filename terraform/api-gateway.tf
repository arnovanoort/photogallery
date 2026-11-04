resource "aws_apigatewayv2_api" "photo_api" {
  name          = "photogallery-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "PUT"]
    allow_headers = ["*"]
    max_age       = 300
  }
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.photo_api.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_apigatewayv2_integration" "lambda_integration" {
  api_id           = aws_apigatewayv2_api.photo_api.id
  integration_type = "AWS_PROXY"
  integration_uri  = aws_lambda_function.album_handler.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "album_route" {
  api_id    = aws_apigatewayv2_api.photo_api.id
  route_key = "ANY /galleries/{galleryId}/album"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

resource "aws_apigatewayv2_route" "albums_route" {
  api_id    = aws_apigatewayv2_api.photo_api.id
  route_key = "ANY /galleries/{galleryId}/albums"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

resource "aws_apigatewayv2_route" "import_route" {
  api_id    = aws_apigatewayv2_api.photo_api.id
  route_key = "ANY /galleries/{galleryId}/import"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}
# Lambda permission enabling call from api gateway
resource "aws_lambda_permission" "api_gw_lambda" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.album_handler.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.photo_api.execution_arn}/*/*/*"
}

