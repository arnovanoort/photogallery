package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.List;
import java.util.Map;
import java.util.Iterator; // Import Iterator

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumHandlerTest {

    private AlbumHandler handler;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DynamoDbEnhancedClient mockEnhancedClient;
    @Mock
    private S3Presigner mockS3Presigner;
    @Mock
    private DynamoDbTable<Album> mockAlbumTable;
    @Mock
    private DynamoDbTable<Photo> mockPhotoTable;
    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        handler = new AlbumHandler(mockEnhancedClient, mockS3Presigner);
        when(mockEnhancedClient.table(anyString(), any(TableSchema.class)))
                .thenAnswer(invocation -> {
                    TableSchema<?> schema = invocation.getArgument(1);
                    if (schema.itemType().rawClass().equals(Album.class)) {
                        return mockAlbumTable;
                    } else if (schema.itemType().equals(Photo.class)) {
                        return mockPhotoTable;
                    }
                    return null;
                });
    }

    @Test
    void handleRequest_AlbumNotFound_Returns404() {
        when(mockAlbumTable.getItem(any(Key.class))).thenReturn(null);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/album");
        request.setHttpMethod("GET");
        request.setQueryStringParameters(Map.of("albumId", "99"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        assertEquals(404, response.getStatusCode());
        assertEquals("Album niet gevonden", response.getBody());
    }

    @Test
    void handleRequest_DefaultAlbumId_IfMissingFromQuery() {
        when(mockAlbumTable.getItem(any(Key.class))).thenReturn(null);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/album");
        request.setHttpMethod("GET");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        assertEquals(404, response.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleRequest_GetAllAlbums_ReturnsAlbumsList() throws Exception {
        Album album1 = Album.builder().albumId("1").naam("Vakantie 2024").locatie("Helmond").datum("2024-07-15").build();
        Album album2 = Album.builder().albumId("2").naam("Familie Feest").locatie("Eindhoven").datum("2024-08-01").build();
        List<Album> albums = List.of(album1, album2);

        // Correcte manier om een SdkIterable te maken van een List
        SdkIterable<Album> mockSdkIterable = new SdkIterable<Album>() {
            @Override
            public Iterator<Album> iterator() {
                return albums.iterator();
            }
        };

        // Mock de PageIterable die door .scan() wordt geretourneerd
        PageIterable<Album> mockPageIterable = mock(PageIterable.class);
        when(mockPageIterable.items()).thenReturn(mockSdkIterable);

        when(mockAlbumTable.scan()).thenReturn(mockPageIterable);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/albums");
        request.setHttpMethod("GET");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));

        // De handler retourneert momenteel een List<Album> direct, niet verpakt in AlbumsListResponse
        assertEquals(objectMapper.writeValueAsString(albums), response.getBody());
    }
}
