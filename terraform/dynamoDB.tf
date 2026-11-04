resource "aws_dynamodb_table" "photogallery_table" {
  name           = "PhotogalleryData"
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

resource "aws_iam_policy" "lambda_s3_dynamo_policy" {
  name        = "LambdaS3DynamoPolicy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action   = "s3:ListBucket"
        Effect   = "Allow"
        Resource = data.aws_s3_bucket.photo_bucket.arn
      },
      {
        Action   = "s3:GetObject"
        Effect   = "Allow"
        Resource = "${data.aws_s3_bucket.photo_bucket.arn}/*"
      },
      {
        Action   = ["dynamodb:GetItem", "dynamodb:Query", "dynamodb:Scan", "dynamodb:PutItem"]
        Effect   = "Allow"
        Resource = aws_dynamodb_table.photogallery_table.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_s3_dynamo_attach" {
  role       = aws_iam_role.photogallery_lambda_exec_role.name
  policy_arn = aws_iam_policy.lambda_s3_dynamo_policy.arn
}
