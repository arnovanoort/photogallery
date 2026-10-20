package nl.arnovanoort.photobook;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photobook.dto.GalleryAlbumResponse;
import nl.arnovanoort.photobook.dto.GalleryResponse;
import nl.arnovanoort.photobook.model.Album;
import nl.arnovanoort.photobook.dto.AlbumRequest;
import nl.arnovanoort.photobook.dto.AlbumResponse;
import nl.arnovanoort.photobook.model.Photo;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Testcontainers
class AlbumHandlerIntegrationTest extends AWSEnabledIntegrationTest {

    /**
     *
     * This test will fire up a local DynamoDB and S3 and on that. In preparation phase some photos are uploaded to the bucket.
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
     *
     * This test will fire up a local DynamoDB and S3 and on that. In preparation phase some photos are uploaded to the bucket.
     * - create a new album
     * - fetch the created album
     * - assert the results
     */
    @Test
    void testImportAlbums() {

        // import the existing photos on S3 for gallery citytrips into dynamoDb
        var importAlbumsResponse = albumHandler.handleRequest(
                createRequest("/citytrips/london/import", testGalleryCitytrips, "POST", null),
                null
        );

        var getAlbumsResponse = albumHandler.handleRequest(
                createRequest("/galleries/test/albums", testGalleryCitytrips, "GET", null),
                null
        );

        assertEquals(200, getAlbumsResponse.getStatusCode());
        GalleryResponse response = parseResponse(getAlbumsResponse.getBody(), new TypeReference<GalleryResponse>() {});
        assertTrue(response.getAlbums().size() == 2);
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
        assertAlbumResponse(parseResponse(getAlbumResponse.getBody(), AlbumResponse.class), testGalleryCitytrips, testAlbumLondonName, new String[] {testPhotoTowerBridge, testPhotoLondoneye});

//        // create new album
//        var createResponse = createTestAlbum(testGalleryCitytrips);
//        Album createdAlbum = parseResponse(createResponse.getBody(), Album.class);
//
//        // test result
//        assertEquals(201, createResponse.getStatusCode());
//        assertEquals(createdAlbum, testAlbum);
//
        // fetch created album
//        var getAlbumResponse = albumHandler.handleRequest(
//                createRequest("/galleries/test/album", testGalleryCitytrips, "GET", null, Map.of("albumId", createdAlbum.getAlbumId())),
//                null
//        );
//
//        log.info("fetch created album response {}", getAlbumResponse.getBody());
//        assertEquals(200, getAlbumResponse.getStatusCode());
//        assertEquals(testAlbumResponse, parseResponse(getAlbumResponse.getBody(), AlbumResponse.class));
    }

    /* Helper method to validate an AlbumResponse
       A response will look like this:
         AlbumResponse(
              album=
                  Album(photobook=citytrips_gallery, albumId=02db7f1b-85fb-4394-a7f5-788cd2c5d9f3, name=london, date=2026-10-20T14:15:37.415484, pk=PHOTOBOOK#citytrips_gallery, sk=ALBUM#02db7f1b-85fb-4394-a7f5-788cd2c5d9f3),
              photos=[
                  Photo(albumId=02db7f1b-85fb-4394-a7f5-788cd2c5d9f3, photoId=3a0b35cf-29d6-41cf-89b8-32e8bd571dc6, title=null, s3FileName=citytrips_gallery/london/londoneye.png, date=2026-10-20T14:15:37.686196800, preSignedUrl=https://photogallerybucket.s3.eu-west-1.amazonaws.com/citytrips_gallery/london/londoneye.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20261020T121538Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAWUBBCWMTP4DZPQGD%2F20261020%2Feu-west-1%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=60da4302c9124c6aee219410dfc16c8f5c742ca514dcc92180d2ef69189d8d75, pk=ALBUM#02db7f1b-85fb-4394-a7f5-788cd2c5d9f3, sk=PHOTO#3a0b35cf-29d6-41cf-89b8-32e8bd571dc6),
                  Photo(albumId=02db7f1b-85fb-4394-a7f5-788cd2c5d9f3, photoId=68d2770c-09df-41ae-9293-dec6c3393d96, title=null, s3FileName=citytrips_gallery/london/towerbridge.png, date=2026-10-20T14:15:37.686196800, preSignedUrl=https://photogallerybucket.s3.eu-west-1.amazonaws.com/citytrips_gallery/london/towerbridge.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20261020T121538Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAWUBBCWMTP4DZPQGD%2F20261020%2Feu-west-1%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=bff020baebd5fc6f1d894f0efe80895423a5ce46eb79afd5a6989b7b76ea94ad, pk=ALBUM#02db7f1b-85fb-4394-a7f5-788cd2c5d9f3, sk=PHOTO#68d2770c-09df-41ae-9293-dec6c3393d96)
                  ]
         )
    */
    private void assertAlbumResponse(AlbumResponse albumResponse, String expectedGalleryName, String expectedAlbumName, String[] expetedPhotoS3Filenames ) {
        var albums = albumResponse.getAlbum();
        var photos = albumResponse.getPhotos();
        assertEquals(expectedGalleryName, albums.getPhotobook());
        assertEquals(expectedAlbumName, albums.getName());
        for(String photoS3Filename: expetedPhotoS3Filenames ){
            assertEquals(photos.stream().map(Photo::getS3FileName).filter(filteredAlbumName -> filteredAlbumName.equals(photoS3Filename)).count(), 1);
        }

    }

    private APIGatewayV2HTTPResponse createTestAlbum(String galleryName) {
        AlbumRequest albumRequest = AlbumRequest.builder()
                .name(testAlbum.getName())
                .albumId(testAlbumLondonId)
                .build();

        var createResult = albumHandler.handleRequest(
                createRequest("/gallery/test/albums", galleryName, "POST", albumRequest),
                null
        );

        return createResult;
    }

    @Test
    void testGetAllAlbums() {
        var createResponse = createTestAlbum(testGalleryCitytrips);
        var response = albumHandler.handleRequest(
                createRequest("/gallery/test/albums", testGalleryCitytrips, "GET", null),
                null
        );

        assertEquals(200, response.getStatusCode());
        assertEquals(testGalleryResponse, parseResponse(response.getBody(), new TypeReference<GalleryResponse>() {}));
    }

    @Test
    void testGetNonExistentAlbum() {
        var response = albumHandler.handleRequest(
                createRequest("/gallery/test/album",  testGalleryCitytrips, "GET", null, Map.of("albumId", testAlbumLondonId)),
                null
        );

        assertEquals(404, response.getStatusCode());
        assertEquals("Album " + testAlbumLondonId + "not found", response.getBody());
    }

    private APIGatewayV2HTTPEvent createRequest(
            String path, String gallery, String method, Object body) {
        return createRequest(path, gallery, method, body, null);
    }

    private APIGatewayV2HTTPEvent createRequest(
            String path, String gallery, String method, Object body, java.util.Map<String, String> queryParams) {
        var request = new APIGatewayV2HTTPEvent();
        request.setRawPath(path);

        // bootst API Gateway route key & path parameters na
        if (path.endsWith("/albums")) {
            request.setRouteKey(method + " /galleries/{galleryId}/albums");
        } else if (path.endsWith("/album")) {
            request.setRouteKey(method + " /galleries/{galleryId}/album");
        } else if (path.endsWith("import")) {
            request.setRouteKey(method + " /galleries/{galleryId}/import");
        }
        request.setPathParameters(Map.of("galleryId", gallery)); // of haal uit path

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
