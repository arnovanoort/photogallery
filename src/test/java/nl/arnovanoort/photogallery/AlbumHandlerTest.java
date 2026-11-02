package nl.arnovanoort.photogallery;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arnovanoort.photogallery.dto.*;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.repository.DynamoRepository;
import nl.arnovanoort.photogallery.service.AlbumService;
import nl.arnovanoort.photogallery.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static nl.arnovanoort.photogallery.ObjectMother.*;

@ExtendWith(MockitoExtension.class)
class AlbumHandlerTest {

    private AlbumHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private DynamoRepository mockDynamoRepository;
    @Mock
    private S3Service mockS3Service;
    @Mock
    private Context context;


    private Album testAlbum;

    @BeforeEach
    void setUp() {
        AlbumService albumService = new AlbumService(mockS3Service, mockDynamoRepository, clock);
        handler = new AlbumHandler(albumService);
        this.testAlbum = anAlbum();
    }

    @Test
    void handleRequest_getAllAlbums_success() throws  Exception{
        when(mockDynamoRepository.getAlbums(AlbumHandler.DEFAULT_GALLERY)).thenReturn(alistOf2Albums());
        APIGatewayV2HTTPEvent request = createGetAlbumsRequest();

        // execute
        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        // assert
        assertEquals(200, response.getStatusCode());
        GalleryResponse galleryResponse  = objectMapper.readValue(response.getBody(), GalleryResponse.class);
        assertEquals(AlbumHandler.DEFAULT_GALLERY, galleryResponse.getName());
        assertIterableEquals(
                galleryResponse
                        .getAlbums()
                        .stream()
                        .map(galleryAlbumResponse -> {
                            return galleryAlbumResponse.toAlbum(galleryResponse.getName());
                        }).toList(),
                alistOf2Albums());

    }
    @Test

    void handleRequest_getAllAlbums_success_for_emnpty_list() throws  Exception{
        when(mockDynamoRepository.getAlbums(AlbumHandler.DEFAULT_GALLERY)).thenReturn(anEmptyListOfAlbums());
        APIGatewayV2HTTPEvent request = createGetAlbumsRequest();

        // execute
        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        // assert
        assertEquals(200, response.getStatusCode());
        GalleryResponse galleryResponse  = objectMapper.readValue(response.getBody(), GalleryResponse.class);
        assertEquals(AlbumHandler.DEFAULT_GALLERY, galleryResponse.getName());
        assertEquals(0, galleryResponse.getAlbums().size());
    }

    @Test
    void handleRequest_getAlbum_album_Found() throws Exception {
        // prepare
        when(mockDynamoRepository.getAlbum(AlbumHandler.DEFAULT_GALLERY, testAlbumId)).thenReturn(Optional.ofNullable(testAlbum));
        when(mockDynamoRepository.getPhotos(testAlbumId)).thenReturn(aListOf2Photos());
        APIGatewayV2HTTPEvent request = createGetAlbumRequest();
        request.setQueryStringParameters(Map.of("albumId", testAlbumId));

        // execute
        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        // assert
        assertEquals(200, response.getStatusCode());
        AlbumResponse errorResponse = objectMapper.readValue(response.getBody(), AlbumResponse.class);
        assertEquals(anAlbum(), errorResponse.getAlbum());
        assertEquals(aListOf2Photos(), errorResponse.getPhotos());
    }

    @Test
    void handleRequest_getAlbum_not_existing_albumId() throws Exception {
        // prepare
        when(mockDynamoRepository.getAlbum(AlbumHandler.DEFAULT_GALLERY, testAlbumId)).thenReturn(Optional.ofNullable(testAlbum));
        when(mockDynamoRepository.getPhotos(testAlbumId)).thenReturn(aListOf2Photos());
        APIGatewayV2HTTPEvent request = createGetAlbumRequest();
        request.setQueryStringParameters(Map.of("albumId", testAlbumId));

        // execute
        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        // assert
        assertEquals(200, response.getStatusCode());
        AlbumResponse errorResponse = objectMapper.readValue(response.getBody(), AlbumResponse.class);
        assertEquals(anAlbum(), errorResponse.getAlbum());
        assertEquals(aListOf2Photos(), errorResponse.getPhotos());
    }

    @Test
    void handleRequest_getAlbum_albumNotFound_returns404() throws Exception {
        // prepare
        when(mockDynamoRepository.getAlbum(AlbumHandler.DEFAULT_GALLERY, testAlbumName)).thenReturn(Optional.empty());
        APIGatewayV2HTTPEvent request = createGetAlbumRequest();
        request.setQueryStringParameters(Map.of("albumId", testAlbumName));

        // execute
        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        // assert
        assertEquals(404, response.getStatusCode());
        ErrorResponse errorResponse = objectMapper.readValue(response.getBody(), ErrorResponse.class);
        assertEquals(404, errorResponse.status());
        assertEquals("Album "+ testAlbumName + " not found", errorResponse.message());
        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void handleRequest_create_album() throws Exception {
        // prepare
        APIGatewayV2HTTPEvent request = createCreateAlbumRequest();
        request.setBody(objectMapper.writeValueAsString(new CreateAlbumRequest(testAlbumName, testAlbumId)));

        // execute
        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        // assert
        assertEquals(201, response.getStatusCode());

        Album album = objectMapper.readValue(response.getBody(), Album.class);
        assertEquals(anAlbum(), album);
    }

    @Test
    void handleRequest_create_album_wrong_body() throws Exception {
        // prepare
        APIGatewayV2HTTPEvent request = createCreateAlbumRequest();
        // give body an album while create album request is expected
        request.setBody(objectMapper.writeValueAsString(testAlbum));

        // execute
        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        // assert
        assertEquals(400, response.getStatusCode());

        ErrorResponse errorResponse = objectMapper.readValue(response.getBody(), ErrorResponse.class);
        assertEquals(errorResponse.message(), "Invalid JSON body");
    }

    @Test
    void handleRequest_import_albums() throws Exception {
        // stub: S3 geeft 2 albums terug
        List<AlbumImport> imports = List.of(
                new AlbumImport("Zomervakantie", List.of("foto1.jpg", "foto2.jpg")),
                new AlbumImport("Kerst", List.of("foto3.jpg"))
        );
        when(mockS3Service.listAlbums(AlbumHandler.DEFAULT_GALLERY)).thenReturn(imports);

        APIGatewayV2HTTPEvent request = createImportRequest();

        // execute
        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        // assert: status en aantal
        assertEquals(201, response.getStatusCode());
        ImportResponse importResponse = objectMapper.readValue(response.getBody(), ImportResponse.class);
        assertEquals(AlbumHandler.DEFAULT_GALLERY, importResponse.getGalleryName());
        assertEquals(2, importResponse.getNrImportedAlbums());

        // assert: repository werd 2x aangeroepen
        verify(mockDynamoRepository, times(2)).importAlbums(any(AlbumImport.class), eq(AlbumHandler.DEFAULT_GALLERY));
    }
    @Test

    void handleRequest_invalid_method() throws Exception {
        // prepare
        APIGatewayV2HTTPEvent request = createInvalidMethodRequest();

        // execute
        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        // assert
        assertEquals(405, response.getStatusCode());

        ErrorResponse errorResponse = objectMapper.readValue(response.getBody(), ErrorResponse.class);
        assertEquals(errorResponse.message(), "Method PUT not allowed for path PUT /galleries/{galleryId}/import");
    }

    private APIGatewayV2HTTPEvent createCreateAlbumRequest(){
        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        APIGatewayV2HTTPEvent.RequestContext.Http http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();

        request.setRawPath("/galleries/test/albums");
        http.setPath("/galleries/test/albums");
        http.setMethod("POST");
        requestContext.setHttp(http);
        request.setRequestContext(requestContext);
        request.setRouteKey("POST /galleries/{galleryId}/albums");
        return request;
    }

    private APIGatewayV2HTTPEvent createGetAlbumRequest(){
        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        APIGatewayV2HTTPEvent.RequestContext.Http http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();

        request.setRawPath("/galleries/test/album");
        http.setPath("/galleries/test/album");
        http.setMethod("GET");
        requestContext.setHttp(http);
        request.setRequestContext(requestContext);
        request.setRouteKey("GET /galleries/{galleryId}/album");
        return request;
    }

    private APIGatewayV2HTTPEvent createGetAlbumsRequest(){
        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        APIGatewayV2HTTPEvent.RequestContext.Http http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();

        request.setRawPath("/galleries/test/albums");
        http.setPath("/galleries/test/albums");
        http.setMethod("GET");
        requestContext.setHttp(http);
        request.setRequestContext(requestContext);
        request.setRouteKey("GET /galleries/{galleryId}/albums");
        return request;
    }

    private APIGatewayV2HTTPEvent createImportRequest() {
        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        APIGatewayV2HTTPEvent.RequestContext.Http http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();

        http.setMethod("GET");
        requestContext.setHttp(http);
        request.setRequestContext(requestContext);
        request.setRouteKey("GET /galleries/{galleryId}/import");
        return request;
    }

    private APIGatewayV2HTTPEvent createInvalidMethodRequest() {
        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        APIGatewayV2HTTPEvent.RequestContext.Http http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();

        http.setMethod("PUT");
        requestContext.setHttp(http);
        request.setRequestContext(requestContext);
        request.setRouteKey("PUT /galleries/{galleryId}/import");
        return request;
    }

}
