package nl.arnovanoort.photobook;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photobook.dto.AlbumImport;
import nl.arnovanoort.photobook.model.Album;
import nl.arnovanoort.photobook.dto.AlbumRequest;
import nl.arnovanoort.photobook.dto.AlbumResponse;
import nl.arnovanoort.photobook.model.Photo;
import nl.arnovanoort.photobook.repository.DynamoRepository;
import nl.arnovanoort.photobook.repository.S3Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class AlbumHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    public final static String USERNAME = "Arno"; // replace in next phase with name fetched from cognito

    private final S3Service s3Service;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    protected DynamoRepository dynamoRepository;
    private Clock clock;

    // Default constructor voor AWS Lambda
    public AlbumHandler() {
        this.dynamoRepository = new DynamoRepository();
        this.s3Service = new S3Service();
        this.clock = Clock.systemDefaultZone();
    }

    // Constructor voor Unit Testing (Dependency Injection)
    public AlbumHandler(DynamoRepository dynamoRepository, S3Service s3Service, Clock clock) {
        this.dynamoRepository = dynamoRepository;
        this.s3Service = s3Service;
        this.clock = clock;
    }

    /* needs refactoring to deal with individual api calls from the lambdas */
    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        String path = request.getRawPath();
        String httpMethod = (request.getRequestContext() != null && request.getRequestContext().getHttp() != null)
                ? request.getRequestContext().getHttp().getMethod()
                : null;
        log.info("Received request: {} {}", httpMethod, path);

        if ("/albums".equals(path)) {
            if ("GET".equals(httpMethod)) {
                return handleGetAllAlbums();
            } else if ("POST".equals(httpMethod)) {
                try {
                    AlbumRequest albumRequest = objectMapper.readValue(request.getBody(), AlbumRequest.class);
                    return handleCreateAlbum(albumRequest);
                } catch (Exception e) {
                    log.error("Failed to parse request body: {}", request.getBody(), e);
                    return createResponse("Invalid JSON body", 400);
                }
            }
        } else if ("/album".equals(path)) {
            if ("GET".equals(httpMethod)) {
                return handleGetSingleAlbum(request);
            }
        } else if ("/import".equals(path)) {
            List<AlbumImport> albums = s3Service.listBuckets();
            albums.forEach(importAlbum -> {
                dynamoRepository.importAlbums(importAlbum,USERNAME);
            });
        }

        log.warn("Method or path not supported: {} {}", httpMethod, path);
        // For other methods or unknown paths
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(405)
                .withBody("Methode " + httpMethod + " niet toegestaan voor pad " + path)
                .build();
    }

    private APIGatewayV2HTTPResponse handleGetAllAlbums() {
        log.info("Fetching all albums for user: {}", USERNAME);
        List<Album> allAlbums = dynamoRepository.getAlbums(USERNAME);
        log.info("Retrieved {} albums for user: {}", allAlbums.size(), USERNAME);
        return create200Response(allAlbums);
    }

    private Optional<String> getAlbumId(APIGatewayV2HTTPEvent request){
        if (request.getQueryStringParameters() != null && request.getQueryStringParameters().containsKey("albumId")) {
            return Optional.ofNullable(request.getQueryStringParameters().get("albumId"));
        } else {
            return Optional.empty();
        }
    }
    private APIGatewayV2HTTPResponse handleGetSingleAlbum(APIGatewayV2HTTPEvent request) {
        Optional<String> albumId = getAlbumId(request);
        log.info("Fetching album: {} for user: {}", albumId, USERNAME);

        return albumId
            // retrieve album with given id
            .map(id -> dynamoRepository.getAlbum(USERNAME, id))
            // extract album and process fotos.
            .map( album -> {
                List<Photo> photos = dynamoRepository.getPhotos("ALBUM#" + album.getAlbumId());
                log.info("Found {} photos for album: {}", photos.size(), album.getAlbumId());

                photos.forEach(photo -> photo.setPreSignedUrl(s3Service.generatePresignedUrl(photo.getS3FileName())));

                AlbumResponse responseBody = AlbumResponse.builder()
                        .album(album)
                        .photos(photos)
                        .build();
                return create200Response(responseBody);
            }).orElseGet(() -> {
                log.warn("Album not found: {} for user: {}", albumId.orElse("unknown"), USERNAME);
                return create404Response("Album " + albumId.orElse("unknown") + "not found");
            });
    }

    private APIGatewayV2HTTPResponse handleCreateAlbum(AlbumRequest albumRequest) {
        String albumId = Optional.ofNullable(albumRequest.getAlbumId())
                .orElseGet(() -> java.util.UUID.randomUUID().toString());

        log.info("Creating album: name='{}', albumId='{}' for user: {}", albumRequest.getName(), albumId, USERNAME);

        Album album = Album.builder()
                .photobook(USERNAME)
                .albumId(albumId)
                .name(albumRequest.getName())
                .date(LocalDateTime.now(clock))
                .pk("PHOTOBOOK#" + USERNAME)
                .sk("ALBUM#" + albumId)
                .build();
        dynamoRepository.createAlbum(album);
        log.info("Successfully created album: {}", albumId);

        return create201Response(album);
    }

    private APIGatewayV2HTTPResponse create200Response(Object responseBody) {
        return createResponse(responseBody, 200);
    }

    private APIGatewayV2HTTPResponse create201Response(Object responseBody) {
        return createResponse(responseBody, 201);
    }

    private APIGatewayV2HTTPResponse create404Response(Object responseBody) {
        return createResponse(responseBody, 404);
    }

    private APIGatewayV2HTTPResponse createResponse(Object responseBody, int statusCode) {
        String body;
        if (responseBody instanceof String stringBody) {
            body = stringBody;
        } else {
            try {
                body = objectMapper.writeValueAsString(responseBody);
            } catch (Exception e) {
                log.error("Failed to serialize response body", e);
                body = "{\"error\": \"Internal Server Error\"}";
                statusCode = 500;
            }
        }

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(Map.of(
                        "Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*"
                ))
                .withBody(body)
                .build();
    }
}
