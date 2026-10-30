package nl.arnovanoort.photogallery.dto;

import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.model.Photo;

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
                .date(LocalDateTime.now())
                .pk("PHOTOGALLERY#" + gallery)
                .sk("ALBUM#" + albumId)
                .build();
    }

    public List<Photo> toPhotos(String albumId) {
        return photoName.stream().map(name -> {
            String photoId = UUID.randomUUID().toString();
            return Photo.builder()
                    .albumId(albumId)
                    .photoId(photoId)
                    .s3FileName(name)
                    .date(LocalDateTime.now().toString())
                    .pk("ALBUM#" + albumId)
                    .sk("PHOTO#" + photoId)
                    .build();
        }).toList();
    }
}
