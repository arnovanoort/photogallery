package nl.arnovanoort.photobook.repository;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arnovanoort.photobook.model.Album;
import nl.arnovanoort.photobook.AlbumHandler;
import nl.arnovanoort.photobook.model.Photo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Iterator; // Import Iterator

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoRepositoryTest {

    private DynamoRepository repository;
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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
        repository = new DynamoRepository(mockEnhancedClient);
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
    @SuppressWarnings("unchecked")
    void handleRequest_GetAllAlbums_ReturnsAlbumsList() throws Exception {
        Album album1 = Album.builder().photobook(AlbumHandler.USERNAME + "#Album").albumId("1").name("Vakantie 2024").date(LocalDateTime.now()).build();
        Album album2 = Album.builder().photobook(AlbumHandler.USERNAME + "#Album").albumId("2").name("Familie Feest").date(LocalDateTime.now()).build();
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

        when(mockAlbumTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);


        List<Album> albumsResult = repository.getAlbums(AlbumHandler.USERNAME);

        // De handler retourneert momenteel een List<Album> direct, niet verpakt in AlbumsListResponse
        assertEquals(albums, albumsResult);
    }
}
