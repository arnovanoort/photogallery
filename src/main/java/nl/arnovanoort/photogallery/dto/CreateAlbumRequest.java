package nl.arnovanoort.photogallery.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlbumRequest {
    private String name;
    private String albumId;
}
