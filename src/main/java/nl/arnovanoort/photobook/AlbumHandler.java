package nl.arnovanoort.photobook;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photobook.dto.AlbumImport;
import nl.arnovanoort.photobook.dto.GalleryResponse;
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

    protected final static String DEFAULT_GALLERY = "demo";
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
        String gallery = getGalleryId(request).orElse("demo");
        String routeKey = request.getRouteKey();
        String httpMethod = (request.getRequestContext() != null && request.getRequestContext().getHttp() != null)
                ? request.getRequestContext().getHttp().getMethod()
                : null;
        log.info("Received request: {} {}", httpMethod, routeKey);

        if (routeKey != null && routeKey.endsWith("/albums")) {
            if ("GET".equals(httpMethod)) {
                return handleGetAllAlbums(gallery);
            } else if ("POST".equals(httpMethod)) {
                try {
                    AlbumRequest albumRequest = objectMapper.readValue(request.getBody(), AlbumRequest.class);
                    return handleCreateAlbum(gallery, albumRequest);
                } catch (Exception e) {
                    log.error("Failed to parse request body: {}", request.getBody(), e);
                    return createResponse("Invalid JSON body", 400);
                }
            }
        } else if (routeKey != null && routeKey.endsWith("/album")) {
            if ("GET".equals(httpMethod)) {
                return handleGetSingleAlbum(gallery, request);
            }
        } else if (routeKey != null && routeKey.endsWith("/import")) {
            List<AlbumImport> albums = s3Service.listAlbums(gallery);
            albums.forEach(importAlbum -> {
                log.info("Importing album: {} for gallery {}", importAlbum, gallery);
                dynamoRepository.importAlbums(importAlbum, gallery);
            });
        }

        log.warn("Method or path not supported: {} {}", httpMethod, routeKey);
        // For other methods or unknown paths
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(405)
                .withBody("Methode " + httpMethod + " niet toegestaan voor pad " + routeKey)
                .build();
    }

    private APIGatewayV2HTTPResponse handleGetAllAlbums(String gallery) {
        log.info("Fetching all albums for user: {}", gallery);
        List<Album> allAlbums = dynamoRepository.getAlbums(gallery);
        log.info("Retrieved {} albums for user: {}", allAlbums.size(), gallery);
        return create200Response(GalleryResponse.fromAlbums(gallery, allAlbums));
    }

    private Optional<String> getAlbumId(APIGatewayV2HTTPEvent request){
        if (request.getQueryStringParameters() != null && request.getQueryStringParameters().containsKey("albumId")) {
            return Optional.ofNullable(request.getQueryStringParameters().get("albumId"));
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> getGalleryId(APIGatewayV2HTTPEvent request) {
        if (request.getPathParameters() != null) {
            return Optional.ofNullable(request.getPathParameters().get("galleryId"));
        }
        return Optional.empty();
    }
    private APIGatewayV2HTTPResponse handleGetSingleAlbum(String gallery, APIGatewayV2HTTPEvent request) {
        Optional<String> albumId = getAlbumId(request);
        log.info("Fetching album: {} for user: {}", albumId, gallery);

        return albumId
            // retrieve album with given id
            .map(id -> dynamoRepository.getAlbum(gallery, id))
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
                log.warn("Album not found: {} for user: {}", albumId.orElse("unknown"), gallery);
                return create404Response("Album " + albumId.orElse("unknown") + "not found");
            });
    }

    private APIGatewayV2HTTPResponse handleCreateAlbum(String gallery, AlbumRequest albumRequest) {
        String albumId = Optional.ofNullable(albumRequest.getAlbumId())
                .orElseGet(() -> java.util.UUID.randomUUID().toString());

        log.info("Creating album: name='{}', albumId='{}' for user: {}", albumRequest.getName(), albumId, gallery);

        Album album = Album.builder()
                .photobook(gallery)
                .albumId(albumId)
                .name(albumRequest.getName())
                .date(LocalDateTime.now(clock))
                .pk("PHOTOBOOK#" + gallery)
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
