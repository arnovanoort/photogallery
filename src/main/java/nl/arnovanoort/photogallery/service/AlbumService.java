package nl.arnovanoort.photogallery.service;

import nl.arnovanoort.photogallery.dto.AlbumImport;
import nl.arnovanoort.photogallery.dto.CreateAlbumRequest;
import nl.arnovanoort.photogallery.dto.AlbumResponse;
import nl.arnovanoort.photogallery.dto.ImportResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.model.PhotoWithoutLink;
import nl.arnovanoort.photogallery.model.Photo;
import nl.arnovanoort.photogallery.repository.AlbumRepository;
import nl.arnovanoort.photogallery.repository.dynamo.DynamoAlbumRepository;
import nl.arnovanoort.photogallery.service.s3.S3StorageService;

@Slf4j
public class AlbumService {

    private StorageService storageService;
    private AlbumRepository albumRepository;
    private Clock clock;

    public AlbumService(){
        this.storageService = new S3StorageService();
        this.albumRepository = new DynamoAlbumRepository();
        this.clock = Clock.systemDefaultZone();
    }

    // constructor for testing
    public AlbumService(StorageService storageService, AlbumRepository albumRepository, Clock clock){
        this.storageService = storageService;
        this.albumRepository = albumRepository;
        this.clock = clock;
    }

    /**
     * This method takes a gallery, will use that to find existing albums on S3
     * and register those (and its photos) in DynamoDb
     *
     * @param gallery
     * @return ImportResponse containing the result of the import
     */
    public ImportResponse importAlbums(String gallery) {
        List<AlbumImport> albums = storageService.listAlbums(gallery);
        albums.forEach(importAlbum -> {
            log.info("Importing album: {} for gallery {}", importAlbum, gallery);
            Album album = importAlbum.toAlbum(gallery);
            List<PhotoWithoutLink> photos = importAlbum.toPhotos(album.getAlbumId());
            albumRepository.createAlbumAndPhotos(album, photos);
        });
        return  ImportResponse.builder()
                .galleryName(gallery)
                .albumNames(albums.stream().map(album -> album.albumName()).toList())
                .build();
    }

    /**
     * Fetch all albums for a gallery from the repository
     *
     * @param gallery
     * @return
     */
    public List<Album> getAlbums(String gallery){
        log.info("Fetching all albums for user: {}", gallery);
        List<Album> allAlbums = albumRepository.getAlbums(gallery);
        log.info("Retrieved {} albums for user: {}", allAlbums.size(), gallery);
        return allAlbums;
    }

    public Optional<AlbumResponse> getAlbum(String gallery, String albumId) {
        return albumRepository.getAlbum(gallery, albumId)
                .map(album -> {
                    // extract album and process photos.
                    List<PhotoWithoutLink> photosWithoutLink = albumRepository.getPhotos(album.getAlbumId());
                    log.info("Found {} photos for album: {}", photosWithoutLink.size(), album.getAlbumId());

                    List<Photo> photos = photosWithoutLink.stream().map(photo -> {
                            String publicLink = storageService.generatePublicURL(gallery, album.getName(), photo.getFilename());
    //                        String publicLink = storageService.generatePublicURL(gallery + "/" + album.getName() + "/" + photo.getFilename());
                        return Photo.fromPhoto(photo, publicLink);
                    }
                    ).toList();

                    return AlbumResponse.builder()
                            .album(album)
                            .photos(photos)
                            .build();
                });
    }

    public Album createAlbum(String gallery, CreateAlbumRequest createAlbumRequest) {
        String albumId = Optional.ofNullable(createAlbumRequest.getAlbumId())
                .orElseGet(() -> java.util.UUID.randomUUID().toString());

        log.info("Creating album: name='{}', albumId='{}' for user: {}", createAlbumRequest.getName(), albumId, gallery);

        // TODO: Album should not know about pk/sk
        Album album = Album
                .builder()
                .gallery(gallery)
                .albumId(albumId)
                .name(createAlbumRequest.getName())
                .date(LocalDate.now(clock))
                .build();
        albumRepository.createAlbum(album);
        log.info("Successfully created album: {}", albumId);
        return album;
    }

}
