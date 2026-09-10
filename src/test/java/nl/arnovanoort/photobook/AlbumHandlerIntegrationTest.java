package nl.arnovanoort.photobook;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
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
                createRequest("/album", "GET", null, Map.of("albumId", createdAlbum.getAlbumId())),
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
                createRequest("/albums", "POST", albumRequest),
                null
        );

        return createResult;
    }

    @Test
    void testGetAllAlbums() {
        var createResponse = createTestAlbum();
        var response = albumHandler.handleRequest(
                createRequest("/albums", "GET", null),
                null

        );

        assertEquals(200, response.getStatusCode());
        assertEquals(List.of(testAlbum), parseResponse(response.getBody(), new TypeReference<List<Album>>() {}));
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

    private APIGatewayV2HTTPEvent createRequest(
            String path, String method, Object body) {
        return createRequest(path, method, body, null);
    }

    private APIGatewayV2HTTPEvent createRequest(
            String path, String method, Object body, java.util.Map<String, String> queryParams) {
        var request = new APIGatewayV2HTTPEvent();
        request.setRawPath(path);

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
