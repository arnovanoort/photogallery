package nl.arnovanoort.photogallery.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Album {
    private String gallery;
    private String albumId;
    private String name;
    private LocalDate  date;

}
