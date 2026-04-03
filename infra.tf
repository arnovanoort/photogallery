# 1. S3 Bucket voor foto's
resource "aws_s3_bucket" "photo_bucket" {
  bucket = "yolo-photobook" # Pas dit aan naar een unieke naam!
}

# Blokkeer publieke toegang (veiligheid eerst)
resource "aws_s3_bucket_public_access_block" "photo_bucket_block" {
  bucket                  = aws_s3_bucket.photo_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# 2. DynamoDB Tabel (Single Table Design)
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
  handler          = "org.example.AlbumHandler::handleRequest"
  runtime          = "java21"
  role             = aws_iam_role.lambda_exec_role.arn
  memory_size      = 512
  timeout          = 30

  # Zorgt dat Terraform wacht tot de JAR is gebouwd
  source_code_hash = fileexists("target/photobook-1.0-SNAPSHOT.jar") ? filebase64sha256("target/photobook-1.0-SNAPSHOT.jar") : null

  environment {
    variables = {
      TABLE_NAME      = aws_dynamodb_table.photobook_table.name
      BUCKET_NAME     = aws_s3_bucket.photo_bucket.id
      ALLOWED_FRIENDS = "jouwemail@gmail.com"
    }
  }
}

# 4. IAM Role & Policies
resource "aws_iam_role" "lambda_exec_role" {
  name = "lambda_exec_role"

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
  role       = aws_iam_role.lambda_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_policy" "lambda_s3_dynamo_policy" {
  name        = "LambdaS3DynamoPolicy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action   = ["s3:GetObject"]
        Effect   = "Allow"
        Resource = "${aws_s3_bucket.photo_bucket.arn}/*"
      },
      {
        Action   = ["dynamodb:GetItem", "dynamodb:Query"]
        Effect   = "Allow"
        Resource = aws_dynamodb_table.photobook_table.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_s3_dynamo_attach" {
  role       = aws_iam_role.lambda_exec_role.name
  policy_arn = aws_iam_policy.lambda_s3_dynamo_policy.arn
}
