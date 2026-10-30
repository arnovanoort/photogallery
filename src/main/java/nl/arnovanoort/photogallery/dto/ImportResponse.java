package nl.arnovanoort.photogallery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {
    private String galleryName;
    private int nrImportedAlbums;
}
