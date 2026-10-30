package nl.arnovanoort.photogallery;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arnovanoort.photogallery.dto.ErrorResponse;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.repository.DynamoRepository;
import nl.arnovanoort.photogallery.service.AlbumService;
import nl.arnovanoort.photogallery.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

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

    private Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    private String testAlbumName = "Test album";
    private Album testAlbum;

    @BeforeEach
    void setUp() {
        AlbumService albumService = new AlbumService(mockS3Service, mockDynamoRepository, clock);
        handler = new AlbumHandler(albumService);
        this.testAlbum = new Album(testAlbumName, "Test description");
    }

    @Test
    void handleRequest_Album_Found() throws Exception {
        // prepare
        when(mockDynamoRepository.getAlbum(AlbumHandler.DEFAULT_GALLERY, testAlbumName)).thenReturn(testAlbum);
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
    void handleRequest_AlbumNotFound_Returns404() throws Exception {
        // prepare
        when(mockDynamoRepository.getAlbum(AlbumHandler.DEFAULT_GALLERY, testAlbumName)).thenReturn(null);
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
}
