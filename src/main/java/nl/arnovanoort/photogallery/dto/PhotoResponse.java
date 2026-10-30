package nl.arnovanoort.photogallery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nl.arnovanoort.photogallery.model.Photo;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class PhotoResponse {
    private String albumId;
    private String photoId;
    private String title;
    private String date;
    private String preSignedUrl;

    public static List<PhotoResponse> fromPhotos(List<Photo> photos){
        return photos.stream().map(photo -> {
            return PhotoResponse.builder()
                    .albumId(photo.getAlbumId())
                    .photoId(photo.getPhotoId())
                    .date(photo.getDate())
                    .preSignedUrl(photo.getPreSignedUrl())
                    .build();
        }).toList();

    }
}
