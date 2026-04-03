# Photobook - Java AWS Lambda Photo App

Een Java-applicatie (AWS Lambda) die foto's uit S3 toont aan een specifieke groep vrienden. Gebruikt DynamoDB voor metadata en S3 Pre-signed URL's voor veilige toegang tot afbeeldingen.

## Architectuur
- **Backend**: Java 21 (AWS SDK v2) in AWS Lambda.
- **Database**: Amazon DynamoDB (Single Table Design).
- **Opslag**: Amazon S3 (Privé bucket).
- **Infrastructuur**: Terraform.
- **Frontend**: HTML/JS (S3 Pre-signed URL's).

## Vereisten
- Java 21 + Maven
- Terraform
- AWS CLI (geconfigureerd met de juiste credentials)

## Bouwen & Testen
Om de applicatie te compileren en de Unit Tests te draaien:
```bash
mvn clean package
```
Dit genereert een "fat JAR" in `target/photobook-1.0-SNAPSHOT.jar`.

## Deployment
De infrastructuur wordt beheerd via Terraform.

1.  **Initialiseer Terraform** (eenmalig):
    ```bash
    terraform init
    ```
2.  **Plan de wijzigingen**:
    ```bash
    terraform plan
    ```
3.  **Voer de deployment uit**:
    ```bash
    terraform apply
    ```
    *Let op: Zorg dat de variabelen in `infra.tf` (zoals `BUCKET_NAME` en `TABLE_NAME`) overeenkomen met je gewenste AWS resources.*

## DynamoDB Data Model (Single Table Design)
| Entiteit | Partition Key (PK) | Sort Key (SK) | Belangrijke Velden |
| :--- | :--- | :--- | :--- |
| **Album** | `ALBUM#<ID>` | `METADATA` | `naam`, `locatie`, `datum` |
| **Photo** | `ALBUM#<ID>` | `PHOTO#<ID>` | `titel`, `s3FileName`, `datum` |
| **Comment** | `PHOTO#<ID>` | `COMMENT#<ID>` | `naam`, `tekst`, `datum` |

## Frontend Gebruik
1.  Open `src/main/resources/static/index.html` in een browser.
2.  Pas in het `<script>` gedeelte de `fetch` URL aan naar je gegenereerde API Gateway endpoint.
3.  Voeg `?albumId=1` toe aan de URL van de pagina om een specifiek album te laden.

## Authenticatie (Toekomst)
De huidige versie is tijdelijk zonder authenticatie voor testdoeleinden. In de volgende fase wordt AWS Cognito (Google OAuth) toegevoegd en de e-mail whitelist controle in de Lambda geactiveerd.
