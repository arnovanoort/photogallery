# 🏗️ Terraform Resource Dependencies & Architectuur

Dit document toont **exact welke Terraform-resource afhangt van welke andere resource** (de dependency tree), opgeknipt in overzichtelijke, leesbare delen.

---

## 1. Hoofdlijnen: De 3 Lagen (Compact)

In plaats van één gigantisch diagram, toont dit overzicht hoe de drie functionele blokken van elkaar afhangen:

```mermaid
flowchart LR
    subgraph L1 ["1. Frontend & DNS"]
        R53["Route 53 Records"] --> CF["CloudFront"]
        CF --> S3_UI["S3 Buckets (UI)"]
    end

    subgraph L2 ["2. API & Routing"]
        CF -. "origin /galleries*" .-> APIGW["API Gateway"]
        APIGW --> L_INT["Integration & Routes"]
        L_INT --> LAMBDA["Lambda Functie"]
    end

    subgraph L3 ["3. IAM & Data"]
        LAMBDA --> IAM["IAM Role & Policies"]
        LAMBDA --> DDB["DynamoDB"]
        LAMBDA --> S3_DATA["S3 Data (Foto's)"]
    end
```

---

## 2. Deeldiagram A: Frontend, DNS & CloudFront

Dit diagram toont uitsluitend de frontend- en CDN-resources en hun directe dependencies:

```mermaid
flowchart TD
    ACM["data.aws_acm_certificate.cert<br/><i>(us-east-1)</i>"]
    S3_P["aws_s3_bucket.photos<br/><i>(yolo-photobook-ui)</i>"]
    OAC["aws_cloudfront_origin_access_control.photos"]
    APIGW["aws_apigatewayv2_api.photo_api"]

    CF["<b>aws_cloudfront_distribution.main</b>"]
    CF -->|viewer_certificate| ACM
    CF -->|origin: photos-origin| S3_P
    CF -->|origin_access_control_id| OAC
    CF -->|origin: api-origin| APIGW

    R53_ROOT["aws_route53_record.root<br/><i>arnovanoort.nl</i>"] -->|alias.domain_name| CF
    R53_WWW["aws_route53_record.www<br/><i>www.arnovanoort.nl</i>"] -->|alias.domain_name| CF

    POL_P["aws_s3_bucket_policy.photos"] -->|bucket| S3_P
    POL_P -->|condition: SourceArn| CF
```

---

## 3. Deeldiagram B: Backend (API Gateway ➡️ Lambda ➡️ IAM ➡️ Data)

Om kruisende pijlen te voorkomen stroomt dit diagram **strikt van boven naar beneden**:
1. **API Gateway & Routing** (boven)
2. **Lambda Compute** (midden)
3. **IAM Autorisatie** (midden-onder)
4. **Opslag: DynamoDB & S3** (onder)

```mermaid
flowchart TD
    subgraph SG_API ["1. API Gateway Laag"]
        API["<b>aws_apigatewayv2_api.photo_api</b><br/><i>(photogallery-api)</i>"]
        STAGE["aws_apigatewayv2_stage.default<br/><i>($default auto-deploy)</i>"]
        INT["aws_apigatewayv2_integration.lambda_integration<br/><i>(AWS_PROXY v2.0)</i>"]
        ROUTES["Routes:<br/>• ANY /galleries/{id}/album<br/>• ANY /galleries/{id}/albums<br/>• ANY /galleries/{id}/import"]
        PERM["<b>aws_lambda_permission.api_gw_lambda</b><br/><i>(AllowAPIGatewayInvoke)</i>"]

        API --> STAGE
        API --> INT
        API --> ROUTES
        API -->|source_arn| PERM
        ROUTES -->|target| INT
    end

    subgraph SG_LAMBDA ["2. Compute Laag"]
        LAMBDA["<b>aws_lambda_function.album_handler</b><br/><i>(photo-album-handler — Java 21)</i>"]
    end

    INT -->|invoke_arn| LAMBDA
    PERM -->|function_name| LAMBDA

    subgraph SG_IAM ["3. IAM & Rechten Laag"]
        ROLE["<b>aws_iam_role.photogallery_lambda_exec_role</b>"]
        
        ATT_BASIC["aws_iam_role_policy_attachment.lambda_basic"]
        POL_BASIC["AWS Managed Policy:<br/>AWSLambdaBasicExecutionRole"]
        
        ATT_CUSTOM["aws_iam_role_policy_attachment.lambda_s3_dynamo_attach"]
        POL_CUSTOM["aws_iam_policy.lambda_s3_dynamo_policy"]

        ROLE --> ATT_BASIC
        POL_BASIC --> ATT_BASIC

        ROLE --> ATT_CUSTOM
        POL_CUSTOM --> ATT_CUSTOM
    end

    LAMBDA -->|role = role.arn| ROLE

    subgraph SG_DATA ["4. Opslag & Data Laag"]
        DDB["<b>aws_dynamodb_table.photogallery_table</b><br/><i>(PhotogalleryData — pk / sk)</i>"]
        S3_DATA["<b>data.aws_s3_bucket.photo_bucket</b><br/><i>(photogallery-data)</i>"]
    end

    LAMBDA -.->|env: TABLE_NAME| DDB
    LAMBDA -.->|env: BUCKET_NAME| S3_DATA

    POL_CUSTOM -->|dynamodb:*| DDB
    POL_CUSTOM -->|s3:GetObject / List| S3_DATA
```

---

## 4. Complete Resource Dependency Matrix

In deze tabel staat voor **elke resource** exact beschreven waar hij op wacht/bouwt, en welk veld gebruikt wordt:

| Terraform Resource | Hangt direct af van (Dependencies) | Gebruikt attribuut |
|---|---|---|
| `aws_route53_record.root` | `data.aws_route53_zone.main`<br/>`aws_cloudfront_distribution.main` | `zone_id`<br/>`domain_name`, `hosted_zone_id` |
| `aws_route53_record.www` | `data.aws_route53_zone.main`<br/>`aws_cloudfront_distribution.main` | `zone_id`<br/>`domain_name`, `hosted_zone_id` |
| `aws_cloudfront_distribution.main` | `data.aws_acm_certificate.cert`<br/>`aws_s3_bucket.photos`<br/>`aws_cloudfront_origin_access_control.photos`<br/>`aws_apigatewayv2_api.photo_api` | `arn`<br/>`bucket_regional_domain_name`<br/>`id`<br/>`id` (voor API domain name) |
| `aws_s3_bucket_policy.photos` | `aws_s3_bucket.photos`<br/>`aws_cloudfront_distribution.main` | `id`<br/>`arn` (in condition SourceArn) |
| `aws_s3_bucket_public_access_block.photos` | `aws_s3_bucket.photos` | `id` |
| `aws_s3_bucket_policy.demo` | `aws_s3_bucket.demo`<br/>`aws_cloudfront_distribution.main` | `id`<br/>`arn` (in condition SourceArn) |
| `aws_apigatewayv2_stage.default` | `aws_apigatewayv2_api.photo_api` | `id` |
| `aws_apigatewayv2_integration.lambda_integration` | `aws_apigatewayv2_api.photo_api`<br/>`aws_lambda_function.album_handler` | `id`<br/>`invoke_arn` |
| `aws_apigatewayv2_route.album_route` | `aws_apigatewayv2_api.photo_api`<br/>`aws_apigatewayv2_integration.lambda_integration` | `id`<br/>`id` |
| `aws_apigatewayv2_route.albums_route` | `aws_apigatewayv2_api.photo_api`<br/>`aws_apigatewayv2_integration.lambda_integration` | `id`<br/>`id` |
| `aws_apigatewayv2_route.import_route` | `aws_apigatewayv2_api.photo_api`<br/>`aws_apigatewayv2_integration.lambda_integration` | `id`<br/>`id` |
| `aws_lambda_permission.api_gw_lambda` | `aws_lambda_function.album_handler`<br/>`aws_apigatewayv2_api.photo_api` | `function_name`<br/>`execution_arn` |
| `aws_lambda_function.album_handler` | `aws_iam_role.photogallery_lambda_exec_role`<br/>`aws_dynamodb_table.photogallery_table`<br/>`data.aws_s3_bucket.photo_bucket` | `arn`<br/>`name` (in env var)<br/>`id` (in env var) |
| `aws_iam_role_policy_attachment.lambda_basic` | `aws_iam_role.photogallery_lambda_exec_role` | `name` |
| `aws_iam_policy.lambda_s3_dynamo_policy` | `data.aws_s3_bucket.photo_bucket`<br/>`aws_dynamodb_table.photogallery_table` | `arn`<br/>`arn` |
| `aws_iam_role_policy_attachment.lambda_s3_dynamo_attach` | `aws_iam_role.photogallery_lambda_exec_role`<br/>`aws_iam_policy.lambda_s3_dynamo_policy` | `name`<br/>`arn` |
| `aws_s3_bucket_public_access_block.photo_bucket_block` | `data.aws_s3_bucket.photo_bucket` | `id` |
