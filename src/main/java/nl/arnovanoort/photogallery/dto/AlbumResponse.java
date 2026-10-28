package nl.arnovanoort.photobook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.arnovanoort.photobook.model.Album;
import nl.arnovanoort.photobook.model.Photo;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumResponse {
    private Album album;
    private List<Photo> photos;
}
