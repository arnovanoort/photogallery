package nl.arnovanoort.photobook;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arnovanoort.photobook.repository.DynamoRepository;
import nl.arnovanoort.photobook.repository.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumHandlerTest {

    private AlbumHandler handler;
    private static String fakeAlbumName = "doesNotExist";

    @Mock
    private DynamoRepository mockDynamoRepository;
    @Mock
    private S3Service mockS3Service;
    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        handler = new AlbumHandler(mockDynamoRepository, mockS3Service);
    }

    @Test
    void handleRequest_AlbumNotFound_Returns404() {
        when(mockDynamoRepository.getAlbum("ALBUM#99")).thenReturn(null);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/album");
        request.setHttpMethod("GET");
        request.setQueryStringParameters(Map.of("albumId", "99"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        assertEquals(404, response.getStatusCode());
        System.out.println("Album niet gevonden " + response.getBody());
        assertEquals("Album niet gevonden", response.getBody());
    }

//    @Test
//    void handleRequest_DefaultAlbumId_IfMissingFromQuery() {
//        when(mockAlbumTable.getItem(any(Key.class))).thenReturn(null);
//
//        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
//        request.setPath("/album");
//        request.setHttpMethod("GET");
//
//        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);
//
//        assertEquals(404, response.getStatusCode());
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    void handleRequest_GetAllAlbums_ReturnsAlbumsList() throws Exception {
//        Album album1 = Album.builder().photobook(AlbumHandler.PHOTOBOOK_NAME + "#Album").albumId("1").naam("Vakantie 2024").locatie("Helmond").datum("2024-07-15").build();
//        Album album2 = Album.builder().photobook(AlbumHandler.PHOTOBOOK_NAME + "#Album").albumId("2").naam("Familie Feest").locatie("Eindhoven").datum("2024-08-01").build();
//        List<Album> albums = List.of(album1, album2);
//
//        // Correcte manier om een SdkIterable te maken van een List
//        SdkIterable<Album> mockSdkIterable = new SdkIterable<Album>() {
//            @Override
//            public Iterator<Album> iterator() {
//                return albums.iterator();
//            }
//        };
//
//        // Mock de PageIterable die door .scan() wordt geretourneerd
//        PageIterable<Album> mockPageIterable = mock(PageIterable.class);
//        when(mockPageIterable.items()).thenReturn(mockSdkIterable);
//
//        when(mockAlbumTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
//
//        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
//        request.setPath("/albums");
//        request.setHttpMethod("GET");
//
//        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);
//
//        assertEquals(200, response.getStatusCode());
//        assertEquals("application/json", response.getHeaders().get("Content-Type"));
//
//        // De handler retourneert momenteel een List<Album> direct, niet verpakt in AlbumsListResponse
//        assertEquals(objectMapper.writeValueAsString(albums), response.getBody());
//    }
}
