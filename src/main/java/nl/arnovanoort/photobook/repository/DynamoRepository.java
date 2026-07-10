package nl.arnovanoort.photobook.repository;

import nl.arnovanoort.photobook.Album;
import nl.arnovanoort.photobook.Photo;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.stream.Collectors;

public class DynamoRepository {
    private final DynamoDbEnhancedClient enhancedClient;
    private final String TABLE_NAME = System.getenv("TABLE_NAME") != null ? System.getenv("TABLE_NAME") : "Photos";

    public DynamoRepository() {
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.create())
                .build();
    }

    public DynamoRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public List<Album> getAlbums(String photobookName) {
        DynamoDbTable<Album> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Album.class));

        return  albumTable.query(QueryConditional.keyEqualTo(
                Key.builder().partitionValue(photobookName + "#Album").build()
        )).items().stream().collect(Collectors.toList());

    }

    public Album getAlbum(String albumName) {
        DynamoDbTable<Album> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Album.class));
        return  albumTable.getItem(Key.builder().partitionValue(albumName).sortValue("METADATA").build());

    }

    public List<Photo> getPhotos(String albumName) {
        DynamoDbTable<Photo> photoTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Photo.class));
        return photoTable.query(QueryConditional.sortBeginsWith(
                        Key.builder().partitionValue(albumName).sortValue("PHOTO#").build()))
                .items().stream().collect(Collectors.toList());

    }

    public void createAlbum(Album album) {
        DynamoDbTable<Album> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Album.class));
        albumTable.putItem(album);

    }
}
