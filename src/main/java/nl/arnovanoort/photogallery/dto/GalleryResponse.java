package nl.arnovanoort.photogallery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.arnovanoort.photogallery.model.Album;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryResponse {
    private String name;
    private List<GalleryAlbumResponse> albums;

    public static GalleryResponse fromAlbums(String galleryName, List<Album> albums){
        return GalleryResponse.builder()
            .name(galleryName)
            .albums(GalleryAlbumResponse.fromAlbums(albums))
            .build();
    }
}




