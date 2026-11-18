package nl.arnovanoort.photogallery.repository.dynamo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.arnovanoort.photogallery.model.Album;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class DynamoAlbum {
    private String gallery;
    private String albumId;
    private String name;
    private LocalDate date;

    private String pk;
    private String sk;


    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("sk")
    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public Album toAlbum() {
        return Album
                .builder()
                .gallery(gallery)
                .albumId(albumId)
                .name(name)
                .date(date)
                .build();
    }

    public static DynamoAlbum fromAlbum(Album album) {
        return DynamoAlbum
                .builder()
                .gallery(album.getGallery())
                .albumId(album.getAlbumId())
                .name(album.getName())
                .date(album.getDate())
                .pk("PHOTOGALLERY#" + album.getGallery())
                .sk("ALBUM#" + album.getAlbumId())
                .build();
    }
}
