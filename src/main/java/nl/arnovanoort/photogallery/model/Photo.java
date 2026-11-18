package nl.arnovanoort.photogallery.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    private String albumId;
    private String photoId;
    private String filename;
    private LocalDateTime date;
    private String publicLink;

    public static Photo fromPhoto(PhotoWithoutLink photo, String publicLink) {
        return Photo
                .builder()
                .albumId(photo.getAlbumId())
                .photoId(photo.getPhotoId())
                .filename(photo.getFilename())
                .date(photo.getDate())
                .publicLink(publicLink)
                .build();
    }
}

