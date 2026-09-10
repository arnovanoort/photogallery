package nl.arnovanoort.photobook;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import nl.arnovanoort.photobook.repository.DynamoRepository;
import nl.arnovanoort.photobook.repository.S3Service;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumHandlerTest {

    private AlbumHandler handler;

    @Mock
    private DynamoRepository mockDynamoRepository;
    @Mock
    private S3Service mockS3Service;
    @Mock
    private Context context;

    private Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @BeforeEach
    void setUp() {
        handler = new AlbumHandler(mockDynamoRepository, mockS3Service, clock);
    }

    @Test
    void handleRequest_AlbumNotFound_Returns404() {
        when(mockDynamoRepository.getAlbum(AlbumHandler.USERNAME, "ALBUM#99")).thenReturn(null);

        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();
        request.setRawPath("/album");

        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        APIGatewayV2HTTPEvent.RequestContext.Http http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        http.setMethod("GET");
        http.setPath("/album");
        requestContext.setHttp(http);
        request.setRequestContext(requestContext);

        request.setQueryStringParameters(Map.of("albumId", "ALBUM#99"));

        APIGatewayV2HTTPResponse response = handler.handleRequest(request, context);

        assertEquals(404, response.getStatusCode());
        assertEquals("Album ALBUM#99not found", response.getBody());
    }
}
