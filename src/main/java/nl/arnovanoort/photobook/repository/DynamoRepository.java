package nl.arnovanoort.photobook.repository;

import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photobook.dto.AlbumImport;
import nl.arnovanoort.photobook.model.Album;
import nl.arnovanoort.photobook.model.Photo;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * each Photo has a partition key of the form 123#Photo For sortkey
 *   METADATA the metadata for that Album will be retrieved
 *   PHOTO@1234 will retrieve a specific foto.
 *   No metadata will retrieve everything for that album(metadata and photos)
 *
 *
 *   Partition Key (PK)	Sort Key (SK)	Type	Album Naam	        Foto URL
 *   PHOTOBOOKNAME#arno ALBUM#123	    Album	"Zomervakantie"	    N/A
 *   PHOTOBOOKNAME#arno ALBUM#456	    Album	"Weekendje weg"	    N/A
 *   ALBUM#123  	    PHOTO#abc	    Photo	N/A	                https://.../abc.jpg
 *   ALBUM#123	        PHOTO#xyz	    Photo	N/A	                https://.../xyz.jpg
 */
@Slf4j
public class DynamoRepository {
    private final DynamoDbEnhancedClient enhancedClient;
    private final String TABLE_NAME = System.getenv("TABLE_NAME") != null ? System.getenv("TABLE_NAME") : "PhotobookData";

    public DynamoRepository() {
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.create())
                .build();
    }

    public DynamoRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    public List<Album> getAlbums(String gallery) {
        log.info("Querying DynamoDB table '{}' for photobook: {}", TABLE_NAME, gallery);
        DynamoDbTable<Album> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Album.class));

        return albumTable.query(QueryConditional.keyEqualTo(
                Key.builder().partitionValue("PHOTOBOOK#" + gallery).build()
        )).items().stream().collect(Collectors.toList());
    }

    public Album getAlbum(String gallery, String albumId) {
        log.info("Querying DynamoDB table '{}' for albumId: {} under gallery: {}", TABLE_NAME, albumId, gallery);
        DynamoDbTable<Album> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Album.class));
        return albumTable.getItem(Key.builder().partitionValue("PHOTOBOOK#" + gallery).sortValue("ALBUM#" + albumId).build());
    }

    public List<Photo> getPhotos(String albumName) {
        log.info("Querying DynamoDB table '{}' for photos under album: {}", TABLE_NAME, albumName);
        DynamoDbTable<Photo> photoTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Photo.class));
        return photoTable.query(QueryConditional.sortBeginsWith(
                        Key.builder().partitionValue(albumName).sortValue("PHOTO#").build()))
                .items().stream().collect(Collectors.toList());
    }

    public void createAlbum(Album album) {
        log.info("Saving album to DynamoDB table '{}': {}", TABLE_NAME, album.getAlbumId());
        DynamoDbTable<Album> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Album.class));
        albumTable.putItem(album);
    }

    private void createPhoto(Photo photo) {
        log.info("Saving photo '{}' to DynamoDB table {}", photo.getS3FileName(), TABLE_NAME);
        DynamoDbTable<Photo> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Photo.class));
        albumTable.putItem(photo);
    }

    public void importAlbums(AlbumImport albumImport, String gallery) {
        Album album = albumImport.toAlbum(gallery);
        createAlbum(album);

        List<Photo> photos = albumImport.toPhotos(album.getAlbumId());
        photos.forEach(photo -> {
            log.info("writing photo {} to dynamo", photo);
            createPhoto(photo);
        });



    }

}
