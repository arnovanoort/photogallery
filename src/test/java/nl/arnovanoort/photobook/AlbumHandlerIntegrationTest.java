package nl.arnovanoort.photobook;

import nl.arnovanoort.photobook.repository.DynamoRepository;
import nl.arnovanoort.photobook.repository.S3Service;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class AlbumHandlerIntegrationTest {


    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8.1")
    ).withServices(LocalStackContainer.Service.DYNAMODB);

    private static DynamoDbClient dynamoDbClient;
    private static DynamoDbEnhancedClient enhancedClient;
    private static DynamoRepository dynamoRepository;
    private static AlbumHandler albumHandler;

    @BeforeAll
    static void setUp() {
        dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                ))
                .region(Region.of(localstack.getRegion()))
                .build();

        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        dynamoRepository = new DynamoRepository(enhancedClient);
        
        S3Service mockS3Service = new S3Service() {
            @Override
            public String generatePresignedUrl(String s3FileName) {
                return "https://fake-presigned-url/" + s3FileName;
            }
        };
        
        albumHandler = new AlbumHandler(dynamoRepository, mockS3Service);

        createTables();
    }

    @AfterAll
    static void tearDown() {
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
        }
    }

    private static void createTables() {
        try {
            dynamoDbClient.createTable(builder -> builder
                    .tableName("Photos")
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("PK")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("SK")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .keySchema(keyBuilder -> keyBuilder
                            .attributeName("PK")
                            .keyType("HASH")
                            .build(),
                            keyBuilder -> keyBuilder
                                    .attributeName("SK")
                                    .keyType("RANGE")
                                    .build()
                    )
                    .billingMode("PAY_PER_REQUEST")
            );
        } catch (Exception e) {
            System.out.println("Table might already exist: " + e.getMessage());
        }
    }

    @Test
    void testCreateAndGetAlbum() {
        AlbumRequest albumRequest = AlbumRequest.builder()
                .naam("Test Album")
                .datum("2024-01-01")
                .build();

        var createResponse = albumHandler.handleRequest(
                createRequest("/albums", "POST", albumRequest),
                null
        );

        assertEquals(201, createResponse.getStatusCode());
        
        Album createdAlbum = parseAlbumFromResponse(createResponse.getBody());
        String albumId = createdAlbum.getAlbumId();

        var getResponse = albumHandler.handleRequest(
                createRequest("/album", "GET", null, Map.of("albumId", albumId)),
                null
        );

        assertEquals(200, getResponse.getStatusCode());
    }

    @Test
    void testGetAllAlbums() {
        var response = albumHandler.handleRequest(
                createRequest("/albums", "GET", null),
                null
        );

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetNonExistentAlbum() {
        var response = albumHandler.handleRequest(
                createRequest("/album", "GET", null, Map.of("albumId", "999999")),
                null
        );

        assertEquals(404, response.getStatusCode());
        assertEquals("Album niet gevonden", response.getBody());
    }

    private com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent createRequest(
            String path, String method, Object body) {
        return createRequest(path, method, body, null);
    }

    private com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent createRequest(
            String path, String method, Object body, java.util.Map<String, String> queryParams) {
        var request = new com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent();
        request.setPath(path);
        request.setHttpMethod(method);
        
        if (body != null) {
            try {
                request.setBody(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        if (queryParams != null) {
            request.setQueryStringParameters(queryParams);
        }
        
        return request;
    }

    private Album parseAlbumFromResponse(String responseBody) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(responseBody, Album.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
