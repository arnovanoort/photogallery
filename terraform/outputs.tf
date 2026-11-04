/*output "demo_bucket_name" {
  value = aws_s3_bucket.demo.bucket
}

output "photos_bucket_name" {
  value = aws_s3_bucket.photos.bucket
}
*/
output "main_cloudfront_domain" {
  value = aws_cloudfront_distribution.main.domain_name
}

output "demo_cloudfront_domain" {
  value = aws_cloudfront_distribution.main.domain_name
}

output "photos_cloudfront_domain" {
  value = aws_cloudfront_distribution.main.domain_name
}