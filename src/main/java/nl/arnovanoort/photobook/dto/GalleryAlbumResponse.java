package nl.arnovanoort.photobook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.arnovanoort.photobook.model.Album;

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

    public static List<GalleryAlbumResponse> fromAlbums(List<Album> albums){
        return albums.stream().map(album -> {
            return GalleryAlbumResponse.builder()
                .name(album.getName())
                .id(album.getAlbumId())
                .date(album.getDate())
                .build();
        }).toList();
    }
}

