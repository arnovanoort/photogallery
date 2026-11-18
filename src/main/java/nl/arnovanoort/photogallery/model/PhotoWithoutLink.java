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
public class PhotoWithoutLink {
    private String albumId;
    private String photoId;
    private String filename;
    private LocalDateTime date;

    public Photo toPhoto(String publicLink) {
        return Photo
                .builder()
                .albumId(albumId)
                .photoId(photoId)
                .filename(filename)
                .date(date)
                .publicLink(publicLink)
                .build();
    }
}

