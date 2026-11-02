package nl.arnovanoort.photogallery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.arnovanoort.photogallery.model.Album;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryAlbumResponse {
    private String name;
    private String id;
    private LocalDateTime date;

    public static GalleryAlbumResponse fromAlbum(Album album){
        //return albums.stream().map(album -> {
            return GalleryAlbumResponse.builder()
                    .name(album.getName())
                    .id(album.getAlbumId())
                    .date(album.getDate())
                    .build();
        //}).toList();
    }

    public Album toAlbum(String galleryName) {
        return Album.builder()
                .gallery(galleryName)
                .name(this.getName())
                .albumId(this.getId())
                .date(this.getDate())
                .pk("PHOTOGALLERY#" + galleryName)
                .sk("ALBUM#" + this.getId())
                .build();
    }
}

