data "aws_s3_bucket" "photo_bucket" {
  bucket = "photogallery-data"
}

resource "aws_s3_bucket_public_access_block" "photo_bucket_block" {
  bucket                  = data.aws_s3_bucket.photo_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
