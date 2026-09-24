# 1. S3 Bucket voor foto's
resource "aws_s3_bucket" "photo_bucket" {
  bucket = "yolo-photobook" # Houd deze naam gelijk aan wat je al gedeployed hebt!
}

# Blokkeer publieke toegang
resource "aws_s3_bucket_public_access_block" "photo_bucket_block" {
  bucket                  = aws_s3_bucket.photo_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# 2. DynamoDB Tabel
resource "aws_dynamodb_table" "photobook_table" {
  name           = "PhotobookData"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "pk"
  range_key      = "sk"

  attribute {
    name = "pk"
    type = "S"
  }

  attribute {
    name = "sk"
    type = "S"
  }
}

# 3. Lambda Functie
resource "aws_lambda_function" "album_handler" {
  function_name    = "photo-album-handler"
  filename         = "target/photobook-1.0-SNAPSHOT.jar"
  handler          = "nl.arnovanoort.photobook.AlbumHandler::handleRequest"
  runtime          = "java21"
  role             = aws_iam_role.photobook_lambda_exec_role.arn
  memory_size      = 512
  timeout          = 30

  source_code_hash = fileexists("target/photobook-1.0-SNAPSHOT.jar") ? filebase64sha256("target/photobook-1.0-SNAPSHOT.jar") : null

  environment {
    variables = {
      TABLE_NAME      = aws_dynamodb_table.photobook_table.name
      BUCKET_NAME     = aws_s3_bucket.photo_bucket.id
    }
  }
}

# 4. IAM Role & Policies
resource "aws_iam_role" "photobook_lambda_exec_role" {
  name = "photobook_lambda_exec_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.photobook_lambda_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_policy" "lambda_s3_dynamo_policy" {
  name        = "LambdaS3DynamoPolicy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action   = "s3:ListBucket"
        Effect   = "Allow"
        Resource = aws_s3_bucket.photo_bucket.arn
      },
      {
        Action   = "s3:GetObject"
        Effect   = "Allow"
        Resource = "${aws_s3_bucket.photo_bucket.arn}/*"
      },
      {
        Action   = ["dynamodb:GetItem", "dynamodb:Query", "dynamodb:Scan", "dynamodb:PutItem"]
        Effect   = "Allow"
        Resource = aws_dynamodb_table.photobook_table.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_s3_dynamo_attach" {
  role       = aws_iam_role.photobook_lambda_exec_role.name
  policy_arn = aws_iam_policy.lambda_s3_dynamo_policy.arn
}

# 5. API Gateway (HTTP API)
resource "aws_apigatewayv2_api" "photo_api" {
  name          = "photobook-api"
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
  route_key = "ANY /album"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

resource "aws_apigatewayv2_route" "albums_route" {
  api_id    = aws_apigatewayv2_api.photo_api.id
  route_key = "ANY /albums"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

resource "aws_apigatewayv2_route" "import_route" {
  api_id    = aws_apigatewayv2_api.photo_api.id
  route_key = "ANY /import"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}
# Lambda permissie om aangeroepen te worden door API Gateway
resource "aws_lambda_permission" "api_gw_lambda" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.album_handler.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.photo_api.execution_arn}/*/*"
}

# Output de API URL
output "api_url_album" {
  value = "${aws_apigatewayv2_stage.default.invoke_url}/album"
}

output "api_url_albums" {
  value = "${aws_apigatewayv2_stage.default.invoke_url}/albums"
}

# Automatic resource import blocks to sync existing AWS infrastructure into state
import {
  to = aws_s3_bucket.photo_bucket
  id = "yolo-photobook"
}

import {
  to = aws_dynamodb_table.photobook_table
  id = "PhotobookData"
}

import {
  to = aws_iam_role.photobook_lambda_exec_role
  id = "lambda_exec_role"
}

import {
  to = aws_lambda_function.album_handler
  id = "photo-album-handler"
}

import {
  to = aws_iam_policy.lambda_s3_dynamo_policy
  id = "arn:aws:iam::455335916326:policy/LambdaS3DynamoPolicy"
}

import {
  to = aws_lambda_permission.api_gw_lambda
  id = "photo-album-handler/AllowAPIGatewayInvoke"
}