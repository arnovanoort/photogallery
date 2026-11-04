resource "aws_lambda_function" "album_handler" {
  function_name    = "photo-album-handler"
  filename         = "${path.module}/../target/photogallery-1.0-SNAPSHOT.jar"
  handler          = "nl.arnovanoort.photogallery.AlbumHandler::handleRequest"
  runtime          = "java21"
  role             = aws_iam_role.photogallery_lambda_exec_role.arn
  memory_size      = 512
  timeout          = 30

  source_code_hash = fileexists("${path.module}/../target/photogallery-1.0-SNAPSHOT.jar") ? filebase64sha256("${path.module}/../target/photogallery-1.0-SNAPSHOT.jar") : null

  environment {
    variables = {
      TABLE_NAME      = aws_dynamodb_table.photogallery_table.name
      BUCKET_NAME     = data.aws_s3_bucket.photo_bucket.id
    }
  }
}
resource "aws_iam_role" "photogallery_lambda_exec_role" {
  name = "photogallery_lambda_exec_role"

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
  role       = aws_iam_role.photogallery_lambda_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}
