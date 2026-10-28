package nl.arnovanoort.photobook.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumRequest {
    private String name;
    private String albumId;
}
