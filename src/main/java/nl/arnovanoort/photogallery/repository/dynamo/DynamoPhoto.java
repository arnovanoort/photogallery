package nl.arnovanoort.photogallery.repository.dynamo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.arnovanoort.photogallery.model.PhotoWithoutLink;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class DynamoPhoto {
    private String albumId;
    private String photoId;
    private String filename;
    private LocalDateTime date;

    private String pk;
    private String sk;


    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() {
        if (pk == null) {
            return "ALBUM#" + albumId;
        }
        return pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("sk")
    public String getSk() {
        if (sk == null) {
            return "PHOTO#" + photoId;
        }
        return sk;
    }

    public PhotoWithoutLink toPhoto() {
        return PhotoWithoutLink.builder()
                .albumId(albumId)
                .photoId(photoId)
                .filename(filename)
                .date(date)
                .build();
    }

    public static DynamoPhoto fromPhoto(PhotoWithoutLink photo) {
        return DynamoPhoto
                .builder()
                .albumId(photo.getAlbumId())
                .photoId(photo.getPhotoId())
                .filename(photo.getFilename())
                .date(photo.getDate())
                .pk("ALBUM#" + photo.getAlbumId())
                .sk("PHOTO#" + photo.getPhotoId())
                .build();
    }

}

