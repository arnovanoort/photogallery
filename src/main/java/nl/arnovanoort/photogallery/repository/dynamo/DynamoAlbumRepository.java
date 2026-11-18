package nl.arnovanoort.photogallery.repository.dynamo;

import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.model.PhotoWithoutLink;
import nl.arnovanoort.photogallery.repository.AlbumRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * each Photo has a partition key of the form 123#Photo For sortkey
 *   METADATA the metadata for that Album will be retrieved
 *   PHOTO@1234 will retrieve a specific photo.
 *   No metadata will retrieve everything for that album(metadata and photos)
 *
 *
 *   Partition Key (PK)	Sort Key (SK)	Type	Album name	        Photo URL
 *   GALLERYNAME#arno   ALBUM#123	    Album	"summerholiday"	    N/A
 *   GALLERYNAME#arno   ALBUM#456	    Album	"weekendtrip"	    N/A
 *   ALBUM#123  	    PHOTO#abc	    Photo	N/A	                https://.../abc.jpg
 *   ALBUM#123	        PHOTO#xyz	    Photo	N/A	                https://.../xyz.jpg
 */
@Slf4j
public class DynamoAlbumRepository implements AlbumRepository {
    private final DynamoDbEnhancedClient enhancedClient;
    private final String TABLE_NAME = System.getenv("TABLE_NAME") != null ? System.getenv("TABLE_NAME") : "PhotogalleryData";

    public DynamoAlbumRepository() {
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.create())
                .build();
    }

    public DynamoAlbumRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    @Override
    public List<Album> getAlbums(String gallery) {
        log.info("Querying DynamoDB table '{}' for gallery: {}", TABLE_NAME, gallery);
        DynamoDbTable<DynamoAlbum> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoAlbum.class));

        return albumTable
                .query(
                        QueryConditional.keyEqualTo(
                                Key.builder().partitionValue("PHOTOGALLERY#" + gallery).build()
                        )
                )

                .items()
                .stream()
                .map(DynamoAlbum::toAlbum)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Album> getAlbum(String gallery, String albumId) {
        log.info("Querying DynamoDB table '{}' for albumId: {} under gallery: {}", TABLE_NAME, albumId, gallery);
        DynamoDbTable<DynamoAlbum> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoAlbum.class));
        return Optional.ofNullable(
            albumTable
                    .getItem(
                            Key.builder()
                                    .partitionValue("PHOTOGALLERY#" + gallery)
                                    .sortValue("ALBUM#" + albumId)
                                    .build()
                    )

        ).map(DynamoAlbum::toAlbum);
    }

    @Override
    public List<PhotoWithoutLink> getPhotos(String albumId) {
        log.info("Querying DynamoDB table '{}' for photos under album: {}", TABLE_NAME, albumId);
        DynamoDbTable<DynamoPhoto> photoTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoPhoto.class));
        return photoTable.query(
                QueryConditional.sortBeginsWith(
                        Key.builder()
                                .partitionValue("ALBUM#" + albumId)
                                .sortValue("PHOTO#").build()
                ))
                .items()
                .stream()
                .map(DynamoPhoto::toPhoto)
                .collect(Collectors.toList());
    }

    @Override
    public void createAlbum(Album album) {
        log.info("Saving album to DynamoDB table '{}': {}", TABLE_NAME, album.getAlbumId());
        DynamoDbTable<DynamoAlbum> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoAlbum.class));
        albumTable.putItem(DynamoAlbum.fromAlbum(album));
    }

    private void createPhoto(PhotoWithoutLink photo) {
        log.info("Saving photo '{}' to DynamoDB table {}", photo.getFilename(), TABLE_NAME);
        DynamoDbTable<DynamoPhoto> albumTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoPhoto.class));
        albumTable.putItem(DynamoPhoto.fromPhoto(photo));
    }

    @Override
    public void createAlbumAndPhotos(Album album, List<PhotoWithoutLink> photos) {
        createAlbum(album);

        photos.forEach(photo -> {
            log.info("writing photo {} to dynamo", photo);
            createPhoto(photo);
        });
    }
}
