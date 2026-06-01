# 1. Variabelen ophalen uit Terraform
$TABLE_NAME = "PhotobookData"
$BUCKET_NAME = "yolo-photobook" # Zorg dat deze naam klopt met je infra.tf!

Write-Host "Gebruik Table: $TABLE_NAME" -ForegroundColor Cyan
Write-Host "Gebruik Bucket: $BUCKET_NAME" -ForegroundColor Cyan

# 2. Een ECHTE test foto uploaden naar S3
# Zorg dat je een bestand genaamd 'dummy_image.jpg' in de root van je project hebt staan!
$imageFileName = "dummy_image.jpg"
if (-not (Test-Path $imageFileName)) {
    Write-Error "Fout: '$imageFileName' niet gevonden in de project root. Plaats een echte JPG/PNG hier."
    exit 1
}
aws s3 cp $imageFileName "s3://$BUCKET_NAME/$imageFileName"
Write-Host "Echte test foto '$imageFileName' geüpload naar S3" -ForegroundColor Green

# 3. Album Metadata toevoegen aan DynamoDB
$albumMetadata = @{
    pk = @{S = "ALBUM#1"}
    sk = @{S = "METADATA"}
    naam = @{S = "Vakantie 2024"}
    locatie = @{S = "Helmond"}
    datum = @{S = "2024-07-15"}
}
$albumJson = $albumMetadata | ConvertTo-Json -Compress
[System.IO.File]::WriteAllText("$PSScriptRoot/temp_album.json", $albumJson)
aws dynamodb put-item --table-name "$TABLE_NAME" --item "file://temp_album.json"
Remove-Item "$PSScriptRoot/temp_album.json"
Write-Host "Album Metadata toegevoegd aan DynamoDB" -ForegroundColor Green

# 4. Foto Metadata toevoegen aan DynamoDB
$photoMetadata = @{
    pk = @{S = "ALBUM#1"}
    sk = @{S = "PHOTO#101"}
    titel = @{S = "Mooie Moestuin"}
    s3FileName = @{S = $imageFileName} # Verwijst nu naar de echte foto
    datum = @{S = "2024-07-16"}
}
$photoJson = $photoMetadata | ConvertTo-Json -Compress
[System.IO.File]::WriteAllText("$PSScriptRoot/temp_photo.json", $photoJson)
aws dynamodb put-item --table-name "$TABLE_NAME" --item "file://temp_photo.json"
Remove-Item "$PSScriptRoot/temp_photo.json"
Write-Host "Foto Metadata toegevoegd aan DynamoDB" -ForegroundColor Green

# Haal de API URL op uit Terraform
$API_URL = terraform output -raw api_url
Write-Host "`nKlaar! Je kunt nu de API testen op: $API_URL?albumId=1" -ForegroundColor Yellow
