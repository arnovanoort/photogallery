package nl.arnovanoort.photobook.dto;

import nl.arnovanoort.photobook.model.Album;
import nl.arnovanoort.photobook.model.Photo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AlbumImport(String albumName, List<String> photoName){
    public Album getAlbum(String photobook){
        String albumId = UUID.randomUUID().toString();
        return Album.builder()
                .photobook(photobook)
                .albumId(albumId)
                .name(albumName)
                .date(LocalDateTime.now())
                .pk("PHOTOBOOK#" + photobook)
                .sk("ALBUM#" + albumId)
                .build();
    }

    public List<Photo> getPhotos(String albumId) {
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
