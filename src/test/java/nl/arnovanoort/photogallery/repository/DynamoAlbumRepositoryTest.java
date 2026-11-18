package nl.arnovanoort.photogallery.repository;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.repository.dynamo.DynamoAlbum;
import nl.arnovanoort.photogallery.repository.dynamo.DynamoAlbumRepository;
import nl.arnovanoort.photogallery.repository.dynamo.DynamoPhoto;
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

import java.util.List;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static nl.arnovanoort.photogallery.ObjectMother.*;

@ExtendWith(MockitoExtension.class)
/*
 * There is not much benefit in the test below. Lots of mocking DynamoClient to check if the album that was
 * put in will also come out. Will leave this class as is for now.
 */

class DynamoAlbumRepositoryTest {

    private DynamoAlbumRepository repository;
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private static final String GALLERY = "demo";
    @Mock
    private DynamoDbEnhancedClient mockEnhancedClient;
    @Mock
    private DynamoDbTable<DynamoAlbum> mockAlbumTable;
    @Mock
    private DynamoDbTable<DynamoPhoto> mockPhotoTable;
    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        repository = new DynamoAlbumRepository(mockEnhancedClient);
        when(mockEnhancedClient.table(anyString(), any(TableSchema.class)))
                .thenAnswer(invocation -> {
                    TableSchema<?> schema = invocation.getArgument(1);
                    if (schema.itemType().rawClass().equals(DynamoAlbum.class)) {
                        return mockAlbumTable;
                    } else if (schema.itemType().equals(DynamoPhoto.class)) {
                        return mockPhotoTable;
                    }
                    return null;
                });
    }


    @Test
    @SuppressWarnings("unchecked")
    void handleRequest_GetAllAlbums_ReturnsAlbumsList() throws Exception {

        /*
            DynamoDB returns its result in pages to prevent too many results
            in our case this is not needed so we merge those pages(using.items())
            The below mocks that response
         */
        SdkIterable<DynamoAlbum> mockSdkIterable = new SdkIterable<DynamoAlbum>() {
            @Override
            public Iterator<DynamoAlbum> iterator() {
                return dynamoAlbums.iterator();
            }
        };

        // mocks returning iterable over the pages(aws returns pages to prevent too many results)
        PageIterable<DynamoAlbum> mockPageIterable = mock(PageIterable.class);

        // we will not get such big results to will merge pages into one result
        when(mockPageIterable.items()).thenReturn(mockSdkIterable);

        when(mockAlbumTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);

        List<Album> albumsResult = repository.getAlbums(GALLERY);

        assertEquals(aListOf2Albums(), albumsResult);
    }
}
