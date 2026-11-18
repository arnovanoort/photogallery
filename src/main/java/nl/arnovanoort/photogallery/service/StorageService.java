package nl.arnovanoort.photogallery.service;

import nl.arnovanoort.photogallery.dto.AlbumImport;

import java.util.List;

public interface StorageService {
    String generatePublicURL(String gallery, String albumName, String filename);

    List<AlbumImport> listAlbums(String galleryName);

    void uploadPhoto(String testGalleryCitytrips, String album, String filename, byte[] bytes);
}
