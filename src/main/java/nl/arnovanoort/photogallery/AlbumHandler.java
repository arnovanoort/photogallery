package nl.arnovanoort.photogallery;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photogallery.dto.*;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.service.AlbumService;

import java.util.List;
import java.util.Optional;

@Slf4j
public class AlbumHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    protected final static String DEFAULT_GALLERY = "demo";
    private final AlbumService albumService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // Default constructor for AWS Lambda
    public AlbumHandler() {
        this.albumService = new AlbumService();
    }

    // Constructor for Testing
    public AlbumHandler(AlbumService albumService) {
        this.albumService = albumService;
    }

    /* needs refactoring to deal with individual api calls from the lambdas */
    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        String gallery = getGalleryId(request).orElse(DEFAULT_GALLERY);
        String routeKey = request.getRouteKey();
        String httpMethod = (request.getRequestContext() != null && request.getRequestContext().getHttp() != null)
                ? request.getRequestContext().getHttp().getMethod()
                : null;
        log.info("Received request: {} {}", httpMethod, routeKey);

        if (routeKey != null && routeKey.endsWith("/albums")) {
            if ("GET".equals(httpMethod)) {
                return handleGetAllAlbums(gallery);
            } else if ("POST".equals(httpMethod)) {
                return handleCreateAlbum(gallery, request);
            }
        } else if (routeKey != null && routeKey.endsWith("/album")) {
            if ("GET".equals(httpMethod)) {
                return handleGetSingleAlbum(gallery, request);
            }
        } else if (routeKey != null && routeKey.endsWith("/import")) {
            return handleImportAlbums(gallery);
        }

        log.warn("Method or path not supported: {} {}", httpMethod, routeKey);
        return ApiResponse.methodNotAllowed("Method " + httpMethod + " not allowed for path " + routeKey);
    }

    private APIGatewayV2HTTPResponse handleImportAlbums(String gallery) {
        ImportResponse importResponse = albumService.importAlbums(gallery);
        return ApiResponse.created(importResponse);
    }

    private APIGatewayV2HTTPResponse handleGetSingleAlbum(String gallery, APIGatewayV2HTTPEvent request) {
        Optional<String> albumIdOp = getAlbumId(request);
        return albumIdOp
                .flatMap(albumId -> {
                    log.info("Fetching album: {} for user: {}", albumId, gallery);
                    return albumService.getAlbum(gallery, albumId);
                })
                .map(response -> ApiResponse.ok(response))
                .orElseGet(() -> {
                    String albumId = albumIdOp.orElse("unknown");
                    log.warn("Cannot find album for id = '{}' and gallery {}", albumId, gallery);
                    return ApiResponse.notFound("Album " + albumId + " not found");
                });
    }

    private APIGatewayV2HTTPResponse handleCreateAlbum(String gallery, APIGatewayV2HTTPEvent request) {
        try {
            AlbumRequest albumRequest = objectMapper.readValue(request.getBody(), AlbumRequest.class);
            Album album = albumService.createAlbum(gallery, albumRequest);
            return ApiResponse.created(album);
        } catch (Exception e) {
            log.error("Failed to parse request body: {}", request.getBody(), e);
            return ApiResponse.badRequest("Invalid JSON body");
        }
    }

    private APIGatewayV2HTTPResponse handleGetAllAlbums(String gallery) {
        List<Album> allAlbums = albumService.getAlbums(gallery);
        return ApiResponse.ok(GalleryResponse.fromAlbums(gallery, allAlbums));
    }

    private Optional<String> getAlbumId(APIGatewayV2HTTPEvent request) {
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


}
