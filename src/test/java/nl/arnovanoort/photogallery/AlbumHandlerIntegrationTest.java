package nl.arnovanoort.photogallery;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photogallery.dto.*;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.model.Photo;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Testcontainers
class AlbumHandlerIntegrationTest extends AWSEnabledIntegrationTest {

    /**
     * This test will fire up a local DynamoDB and S3. In preparation phase some photos are uploaded to the bucket.
     * - create a new album
     * - fetch the created album
     * - assert the results
     */
    @Test
    void testCreateAndGetAlbum() {
        // create new album
        var createResponse = createTestAlbum(testGalleryCitytrips);
        Album createdAlbum = parseResponse(createResponse.getBody(), Album.class);

        // test result
        assertEquals(201, createResponse.getStatusCode());
        assertEquals(createdAlbum, testAlbum);

        // fetch created album
        var getAlbumResponse = albumHandler.handleRequest(
                createRequest("/gallery/test/album", testGalleryCitytrips, "GET", null, Map.of("albumId", createdAlbum.getAlbumId())),
                null
        );

        log.info("fetch created album response {}", getAlbumResponse.getBody());
        assertEquals(200, getAlbumResponse.getStatusCode());
        assertEquals(testAlbumResponse, parseResponse(getAlbumResponse.getBody(), AlbumResponse.class));
    }

    /**
     * This test will fire up a local DynamoDB and S3. In preparation phase some photos are uploaded to the bucket.
     * - import existing albums from S3
     * - fetch created albums
     * - assert the results
     */
    @Test
    void testImportAlbums() {
        // import the existing photos on S3 for gallery citytrips into dynamoDb
        var importAlbumsResponse = albumHandler.handleRequest(
                createRequest("/citytrips/london/import", testGalleryCitytrips, "POST", null),
                null
        );
        assertEquals(201, importAlbumsResponse.getStatusCode());
        assertEquals(testImportResponse, parseResponse(importAlbumsResponse.getBody(), ImportResponse.class));

        var getAlbumsResponse = albumHandler.handleRequest(
                createRequest("/galleries/test/albums", testGalleryCitytrips, "GET", null),
                null
        );

        assertEquals(200, getAlbumsResponse.getStatusCode());
        GalleryResponse response = parseResponse(getAlbumsResponse.getBody(), new TypeReference<GalleryResponse>() {});
        assertEquals(2, response.getAlbums().size());
        assertTrue(response.getAlbums().stream().map(GalleryAlbumResponse::getName).toList().contains("london"));
        assertTrue(response.getAlbums().stream().map(GalleryAlbumResponse::getName).toList().contains("paris"));

        // fetch created album
        String albumId = response.getAlbums().stream().filter(album -> album.getName().equalsIgnoreCase("london")).toList().get(0).getId();
        var getAlbumResponse = albumHandler.handleRequest(
                createRequest("/galleries/test/album", testGalleryCitytrips, "GET", null, Map.of("albumId", albumId)),
                null
        );

        log.info("fetch created album response {}", getAlbumResponse.getBody());
        assertEquals(200, getAlbumResponse.getStatusCode());
        assertAlbumResponse(parseResponse(getAlbumResponse.getBody(), AlbumResponse.class), testGalleryCitytrips, testAlbumLondonName, new String[] {testPhotoTowerBridge, testPhotoLondoneye}, new String[] { testPresignedUrlLondonEye, testPresignedUrlTowerBridge});
    }

    /*
    Helper method to validate an AlbumResponse
    */
    private void assertAlbumResponse(AlbumResponse albumResponse, String expectedGalleryName, String expectedAlbumName, String[] expetedPhotoS3Filenames, String[] expectedPresignedUrls ) {
        var albums = albumResponse.getAlbum();
        var photos = albumResponse.getPhotos();
        assertEquals(expectedGalleryName, albums.getGallery());
        assertEquals(expectedAlbumName, albums.getName());
        assertIterableEquals(
                photos.stream().map(Photo::getS3FileName).sorted().toList(),
                Arrays.stream(expetedPhotoS3Filenames).sorted().toList()
        );
        assertIterableEquals(
                photos.stream().map(Photo::getPreSignedUrl).map(presignedUrl -> presignedUrl.split("\\?")[0]).sorted().toList(),
                Arrays.stream(expectedPresignedUrls).sorted().toList()
        );
    }

    private APIGatewayV2HTTPResponse createTestAlbum(String galleryName) {
        AlbumRequest albumRequest = AlbumRequest.builder()
                .name(testAlbum.getName())
                .albumId(testAlbumLondonId)
                .build();

        return albumHandler.handleRequest(
                createRequest("/gallery/test/albums", galleryName, "POST", albumRequest),
                null
        );
    }

    @Test
    void testGetNonExistentAlbum() {
        var response = albumHandler.handleRequest(
                createRequest("/gallery/test/album", testGalleryCitytrips, "GET", null, Map.of("albumId", testAlbumLondonId)),
                null
        );

        assertEquals(404, response.getStatusCode());
        ErrorResponse error = parseResponse(response.getBody(), ErrorResponse.class);
        assertEquals(404, error.status());
        assertEquals("Album " + testAlbumLondonId + " not found", error.message());
        assertNotNull(error.timestamp());
    }

    private APIGatewayV2HTTPEvent createRequest(
            String path, String gallery, String method, Object body) {
        return createRequest(path, gallery, method, body, null);
    }

    private APIGatewayV2HTTPEvent createRequest(
            String path, String gallery, String method, Object body, java.util.Map<String, String> queryParams) {
        var request = new APIGatewayV2HTTPEvent();
        request.setRawPath(path);

        // Mimics API Gateway route key & path parameters
        if (path.endsWith("/albums")) {
            request.setRouteKey(method + " /galleries/{galleryId}/albums");
        } else if (path.endsWith("/album")) {
            request.setRouteKey(method + " /galleries/{galleryId}/album");
        } else if (path.endsWith("import")) {
            request.setRouteKey(method + " /galleries/{galleryId}/import");
        }
        request.setPathParameters(Map.of("galleryId", gallery));

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
