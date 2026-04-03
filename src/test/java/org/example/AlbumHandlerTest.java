package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumHandlerTest {

    private AlbumHandler handler;

    @Mock
    private DynamoDbEnhancedClient mockEnhancedClient;
    @Mock
    private S3Presigner mockS3Presigner;
    @Mock
    private DynamoDbTable<Album> mockAlbumTable;
    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        handler = new AlbumHandler(mockEnhancedClient, mockS3Presigner);
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleRequest_AlbumNotFound_Returns404() {
        // Gebruik een specifiekere type hint voor Mockito
        when(mockEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(mockAlbumTable);
        when(mockAlbumTable.getItem(any(Key.class))).thenReturn(null);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(Map.of("albumId", "99"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        assertEquals(404, response.getStatusCode());
        assertEquals("Album niet gevonden", response.getBody());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleRequest_DefaultAlbumId_IfMissingFromQuery() {
        when(mockEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(mockAlbumTable);
        when(mockAlbumTable.getItem(any(Key.class))).thenReturn(null);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        assertEquals(404, response.getStatusCode());
    }
}
