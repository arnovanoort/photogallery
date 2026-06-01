package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlbumHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final String TABLE_NAME = System.getenv("TABLE_NAME") != null ? System.getenv("TABLE_NAME") : "Photos";
    private final String BUCKET_NAME = System.getenv("BUCKET_NAME") != null ? System.getenv("BUCKET_NAME") : "photo-bucket";

    private final DynamoDbEnhancedClient enhancedClient;
    private final S3Presigner s3Presigner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Default constructor voor AWS Lambda
    public AlbumHandler() {
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.create())
                .build();
        this.s3Presigner = S3Presigner.create();
    }

    // Constructor voor Unit Testing (Dependency Injection)
    public AlbumHandler(DynamoDbEnhancedClient enhancedClient, S3Presigner s3Presigner) {
        this.enhancedClient = enhancedClient;
        this.s3Presigner = s3Presigner;
    }

    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String path = request.getPath();

        if ("/albums".equals(path)) {
            return handleGetAllAlbums();
        } else if ("/album".equals(path)) {
            return handleGetSingleAlbum(request);
        } else {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Onbekende route: " + path);
        }
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGetAllAlbums() {
        DynamoDbTable<Album> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Album.class));
        List<Album> allAlbums = new ArrayList<>();

        // Scan is duur, maar voor een kleine lijst albums acceptabel.
        // Beter is een GSI als de lijst groot wordt.
        albumTable.scan().items().forEach(allAlbums::add);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of(
                        "Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*"
                ))
                .withBody(objectMapper.writeValueAsString(allAlbums));
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGetSingleAlbum(APIGatewayProxyRequestEvent request) {
        String albumId = "1"; // Default waarde
        if (request.getQueryStringParameters() != null && request.getQueryStringParameters().containsKey("albumId")) {
            albumId = request.getQueryStringParameters().get("albumId");
        }

        DynamoDbTable<Album> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Album.class));
        Album album = albumTable.getItem(Key.builder().partitionValue("ALBUM#" + albumId).sortValue("METADATA").build());

        if (album == null) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withBody("Album niet gevonden");
        }

        DynamoDbTable<Photo> photoTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Photo.class));
        List<Photo> photos = photoTable.query(QueryConditional.sortBeginsWith(
                Key.builder().partitionValue("ALBUM#" + albumId).sortValue("PHOTO#").build()))
                .items().stream().collect(Collectors.toList());

        photos.forEach(photo -> photo.setPreSignedUrl(generatePresignedUrl(photo.getS3FileName())));

        AlbumResponse responseBody = AlbumResponse.builder()
                .album(album)
                .photos(photos)
                .build();

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of(
                        "Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*"
                ))
                .withBody(objectMapper.writeValueAsString(responseBody));
    }

    private String generatePresignedUrl(String s3FileName) {
        if (s3FileName == null || s3FileName.isEmpty()) return null;
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(s3FileName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            return null;
        }
    }
}
