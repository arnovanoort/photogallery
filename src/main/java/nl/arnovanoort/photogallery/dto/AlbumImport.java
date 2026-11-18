package nl.arnovanoort.photogallery.dto;

import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.model.PhotoWithoutLink;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AlbumImport(String albumName, List<String> photoName){
    public Album toAlbum(String gallery){
        String albumId = UUID.randomUUID().toString();
        return Album.builder()
                .gallery(gallery)
                .albumId(albumId)
                .name(albumName)
                .date(LocalDate.now())
                .build();
    }

    public List<PhotoWithoutLink> toPhotos(String albumId) {
        return photoName.stream().map(name -> {
            String photoId = UUID.randomUUID().toString();
            return PhotoWithoutLink.builder()
                    .albumId(albumId)
                    .photoId(photoId)
                    .filename(name)
                    .date(LocalDateTime.now())
                    .build();
        }).toList();
    }
}
