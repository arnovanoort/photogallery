package nl.arnovanoort.photobook;

import nl.arnovanoort.photobook.model.Album;
import nl.arnovanoort.photobook.dto.AlbumResponse;
import nl.arnovanoort.photobook.repository.DynamoRepository;
import nl.arnovanoort.photobook.repository.S3Service;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Contains generic setup for AWS integration tests
 */
abstract class AWSEnabledIntegrationTest {
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8.1")
    ).withServices(LocalStackContainer.Service.DYNAMODB);

    protected static DynamoDbClient dynamoDbClient;
    protected static DynamoDbEnhancedClient enhancedClient;
    protected static DynamoRepository dynamoRepository;
    protected static AlbumHandler albumHandler;
    protected static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    protected static Clock albumCreationDateTimeClock;
    @BeforeAll
    static void setUp() {
        // prepare mocked dynamoDB client
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

        // prepare mocked s3 service
        S3Service mockS3Service = new S3Service() {
            @Override
            public String generatePresignedUrl(String s3FileName) {
                return "https://fake-presigned-url/" + s3FileName;
            }
        };

        // clock used to inject into an (to be created) album
        albumCreationDateTimeClock = Clock.fixed(Instant.parse("2026-08-28T13:30:00Z"), ZoneId.of("UTC"));

        // create the album handler with mocked repo/s3 and the albumCreationDateTime
        albumHandler = new AlbumHandler(dynamoRepository, mockS3Service, albumCreationDateTimeClock);

        // create and setup dynamoDB tables
        createMockedDynamoTables();
    }

    @AfterAll
    static void tearDown() {
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
        }
    }

    private static void createMockedDynamoTables() {
        try {
            dynamoDbClient.createTable(builder -> builder
                    .tableName("PhotobookData")
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("pk")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("sk")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .keySchema(keyBuilder -> keyBuilder
                                    .attributeName("pk")
                                    .keyType("HASH")
                                    .build(),
                            keyBuilder -> keyBuilder
                                    .attributeName("sk")
                                    .keyType("RANGE")
                                    .build()
                    )
                    .billingMode("PAY_PER_REQUEST")
            );
        } catch (Exception e) {
            System.out.println("Table might already exist: " + e.getMessage());
        }
    }

    protected  <T> T parseResponse(String responseBody, Class<T> valueType) {
        try {
            return objectMapper.readValue(responseBody, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T parseResponse(String responseBody, TypeReference<T> valueType) {
        try {
            return objectMapper.readValue(responseBody, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String albumId = "71094182-8da0-4a8a-8f01-d8c98696ab50";
    Album testAlbum = new Album(
        AlbumHandler.GALLERY,
        albumId,
        "test album",
        LocalDateTime.now(albumCreationDateTimeClock),
        "PHOTOBOOK#" + AlbumHandler.GALLERY,
        ("ALBUM#"+ albumId)

    );

    AlbumResponse testAlbumResponse = new AlbumResponse(
        testAlbum,
        List.of()
    );

}
