package nl.arnovanoort.photogallery;

import nl.arnovanoort.photogallery.dto.GalleryAlbumResponse;
import nl.arnovanoort.photogallery.dto.GalleryResponse;
import nl.arnovanoort.photogallery.dto.ImportResponse;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.dto.AlbumResponse;
import nl.arnovanoort.photogallery.repository.AlbumRepository;
import nl.arnovanoort.photogallery.repository.dynamo.DynamoAlbumRepository;
import nl.arnovanoort.photogallery.service.AlbumService;
import nl.arnovanoort.photogallery.service.s3.S3StorageService;
import nl.arnovanoort.photogallery.service.StorageService;
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
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.time.*;
import java.util.Arrays;
import java.util.List;

/**
 * Contains generic setup for AWS integration tests
 */
abstract class AbstractAWSEnabledIntegrationTest {
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8.1")
    ).withServices(
            LocalStackContainer.Service.DYNAMODB,
            LocalStackContainer.Service.S3
    );

    protected static DynamoDbClient dynamoDbClient;
    protected static DynamoDbEnhancedClient enhancedClient;
    protected static AlbumRepository albumRepository;
    protected static StorageService storageService;
    protected static AlbumHandler albumHandler;
    protected static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    protected static Clock albumCreationDateTimeClock;

    protected static String testGallerySports = "sports_gallery";
    protected static String testAlbumBaseball = "baseball";
    protected static String testPhotoPitching = "pitching.png";
    protected static String testPhotoBatting = "batting.png";
    protected static String testGalleryCitytrips = "citytrips_gallery";
    protected static String testAlbumLondonName = "london";
    protected static String testPhotoTowerBridge = "towerbridge.png";
    protected static String testPhotoLondoneye = "londoneye.png";
    protected static String testAlbumParisName = "paris";
    protected static String testPresignedUrlLondonEye   = "https://photogallerybucket.s3.eu-west-1.amazonaws.com/" + testGalleryCitytrips + "/" + testAlbumLondonName + "/" + testPhotoLondoneye;
    protected static String testPresignedUrlTowerBridge = "https://photogallerybucket.s3.eu-west-1.amazonaws.com/" + testGalleryCitytrips + "/" + testAlbumLondonName + "/" + testPhotoTowerBridge;
    protected static String testBucketName = "photogallerybucket";
    protected static String testAlbumLondonId = "71094182-8da0-4a8a-8f01-d8c98696ab50";
    protected static String testAlbumParisId  = "b7d131b2-0958-828b-9b90-943f55df0902";

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

        albumRepository = new DynamoAlbumRepository(enhancedClient);

        // prepare  s3 service
        S3Client s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                ))
                .region(Region.of(localstack.getRegion()))
                .build();
        s3Client.createBucket(b -> b.bucket(testBucketName));
        storageService = new S3StorageService(testBucketName, s3Client);

        // clock used to inject into an (to be created) album
        albumCreationDateTimeClock = Clock.fixed(Instant.parse("2026-08-28T13:30:00Z"), ZoneId.of("UTC"));

        AlbumService albumService = new AlbumService(storageService, albumRepository, albumCreationDateTimeClock);
        // create the album handler with mocked repo/s3 and the albumCreationDateTime
        albumHandler = new AlbumHandler(albumService);

        // create and setup dynamoDB tables
        createMockedDynamoTables();

        // create bucket folders and photos.
        createBucketFoldersAndPhotos();
    }


    @AfterAll
    static void tearDown() {
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
        }
    }

    private static void createBucketFoldersAndPhotos() {
        storageService.uploadPhoto(testGalleryCitytrips, testAlbumLondonName, "londoneye.png"  , readFile("images/citytrips_gallery/london/londoneye.png"));
        storageService.uploadPhoto(testGalleryCitytrips, testAlbumLondonName, "towerbridge.png", readFile("images/citytrips_gallery/london/towerbridge.png"));
        storageService.uploadPhoto(testGalleryCitytrips, testAlbumParisName,  "louvre.png"     , readFile("images/citytrips_gallery/paris/louvre.png"));
        storageService.uploadPhoto(testGalleryCitytrips, testAlbumParisName,  "paris.png"      , readFile("images/citytrips_gallery/paris/paris.png"));
        storageService.uploadPhoto(testGallerySports, testAlbumBaseball,  "batting.png"      , readFile("images/citytrips_gallery/paris/paris.png"));
        storageService.uploadPhoto(testGallerySports, testAlbumBaseball,  "pitching.png"      , readFile("images/citytrips_gallery/paris/paris.png"));
    }

    private static byte[] readFile(String resourcePath) {
        try{
            InputStream is = AbstractAWSEnabledIntegrationTest.class.getResourceAsStream(resourcePath);
            if (is == null) {
                throw new IllegalArgumentException("Resource not found for path: " + resourcePath);
            }

            return is.readAllBytes();
            }
        catch (IOException e){
            throw new RuntimeException("error reading test photo: " + resourcePath, e);
        }
    }

    private static void createMockedDynamoTables() {
        try {
            dynamoDbClient.createTable(builder -> builder
                    .tableName("PhotogalleryData")
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

    Album testAlbum = new Album(
        testGalleryCitytrips,
        testAlbumLondonId,
        testAlbumLondonName,
        LocalDate.now(albumCreationDateTimeClock)
    );

    AlbumResponse testAlbumResponse = new AlbumResponse(
        testAlbum,
        List.of()
    );

    GalleryAlbumResponse londonAlbumResponse = new GalleryAlbumResponse(
            testAlbumLondonName,
            testAlbumLondonId,
            LocalDate.now(albumCreationDateTimeClock)
    );

    GalleryAlbumResponse parisAlbumResponse = new GalleryAlbumResponse(
            testAlbumParisName,
            testAlbumParisId,
            LocalDate.now(albumCreationDateTimeClock)
    );

    GalleryResponse testGalleryResponse = new GalleryResponse(
            testGalleryCitytrips ,
            List.of(londonAlbumResponse, parisAlbumResponse)
    );

    ImportResponse testImportResponse = new ImportResponse(
            testGalleryCitytrips,
            Arrays.asList(new String[]{testAlbumLondonName, testAlbumParisName})
    );

}
