package nl.arnovanoort.photogallery.repository;

import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.model.PhotoWithoutLink;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository {
    List<Album> getAlbums(String gallery);

    Optional<Album> getAlbum(String gallery, String albumId);

    List<PhotoWithoutLink> getPhotos(String albumId);

    void createAlbum(Album album);

    void createAlbumAndPhotos(Album album, List<PhotoWithoutLink> photos);
}
