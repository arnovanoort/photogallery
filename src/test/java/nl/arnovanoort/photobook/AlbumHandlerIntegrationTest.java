package nl.arnovanoort.photobook;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photobook.dto.GalleryResponse;
import nl.arnovanoort.photobook.model.Album;
import nl.arnovanoort.photobook.dto.AlbumRequest;
import nl.arnovanoort.photobook.dto.AlbumResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Testcontainers
class AlbumHandlerIntegrationTest extends AWSEnabledIntegrationTest {

    /**
     * This test will fire up a local DynamoDB and S3 and on that
     * - create a new album
     * - fetch the created album
     * - assert the results
     */
    @Test
    void testCreateAndGetAlbum() {
        // create new album
        var createResponse = createTestAlbum();
        Album createdAlbum = parseResponse(createResponse.getBody(), Album.class);

        // test result
        assertEquals(201, createResponse.getStatusCode());
        assertEquals(createdAlbum, testAlbum);

        // fetch created album
        var getAlbumResponse = albumHandler.handleRequest(
                createRequest("/gallery/test/album", "GET", null, Map.of("albumId", createdAlbum.getAlbumId())),
                null
        );

        log.info("fetch created album response {}", getAlbumResponse.getBody());
        assertEquals(200, getAlbumResponse.getStatusCode());
        assertEquals(testAlbumResponse, parseResponse(getAlbumResponse.getBody(), AlbumResponse.class));
    }

    private APIGatewayV2HTTPResponse createTestAlbum() {
        AlbumRequest albumRequest = AlbumRequest.builder()
                .name(testAlbum.getName())
                .albumId(albumId)
                .build();

        var createResult = albumHandler.handleRequest(
                createRequest("/gallery/test/albums", "POST", albumRequest),
                null
        );

        return createResult;
    }

    @Test
    void testGetAllAlbums() {
        var createResponse = createTestAlbum();
        var response = albumHandler.handleRequest(
                createRequest("/gallery/test/albums", "GET", null),
                null

        );

        assertEquals(200, response.getStatusCode());
        assertEquals(testGalleryResponse, parseResponse(response.getBody(), new TypeReference<GalleryResponse>() {}));
    }

    @Test
    void testGetNonExistentAlbum() {
        var response = albumHandler.handleRequest(
                createRequest("/gallery/test/album", "GET", null, Map.of("albumId", albumId)),
                null
        );

        assertEquals(404, response.getStatusCode());
        assertEquals("Album " + albumId + "not found", response.getBody());
    }

    private APIGatewayV2HTTPEvent createRequest(
            String path, String method, Object body) {
        return createRequest(path, method, body, null);
    }

    private APIGatewayV2HTTPEvent createRequest(
            String path, String method, Object body, java.util.Map<String, String> queryParams) {
        var request = new APIGatewayV2HTTPEvent();
        request.setRawPath(path);

        // bootst API Gateway route key & path parameters na
        if (path.endsWith("/albums")) {
            request.setRouteKey(method + " /galleries/{galleryId}/albums");
        } else if (path.endsWith("/album")) {
            request.setRouteKey(method + " /galleries/{galleryId}/album");
        }
        request.setPathParameters(Map.of("galleryId", "test")); // of haal uit path

        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        APIGatewayV2HTTPEvent.RequestContext.Http http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        http.setMethod(method);
        http.setPath(path);
        requestContext.setHttp(http);
        request.setRequestContext(requestContext);
        
        if (body != null) {
            try {
                request.setBody(objectMapper.writeValueAsString(body));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        if (queryParams != null) {
            request.setQueryStringParameters(queryParams);
        }
        
        return request;
    }
}
