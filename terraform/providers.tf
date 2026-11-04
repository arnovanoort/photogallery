terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "eu-west-1"
}

# extra provider to us-east-1 for the certificate:
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}