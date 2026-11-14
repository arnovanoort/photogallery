package nl.arnovanoort.photogallery.service;

import nl.arnovanoort.photogallery.dto.AlbumImport;
import nl.arnovanoort.photogallery.dto.CreateAlbumRequest;
import nl.arnovanoort.photogallery.dto.AlbumResponse;
import nl.arnovanoort.photogallery.dto.ImportResponse;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.model.Photo;
import nl.arnovanoort.photogallery.repository.DynamoRepository;

@Slf4j
public class AlbumService {

    private S3Service s3Service;
    private DynamoRepository dynamoRepository;
    private Clock clock;

    public AlbumService(){
        this.s3Service = new S3Service();
        this.dynamoRepository = new DynamoRepository();
        this.clock = Clock.systemDefaultZone();
    }

    // constructor for testing
    public AlbumService(S3Service s3Service, DynamoRepository dynamoRepository, Clock clock){
        this.s3Service = s3Service;
        this.dynamoRepository = dynamoRepository;
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
        List<AlbumImport> albums = s3Service.listAlbums(gallery);
        albums.forEach(importAlbum -> {
            log.info("Importing album: {} for gallery {}", importAlbum, gallery);
            dynamoRepository.importAlbums(importAlbum, gallery);
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
        List<Album> allAlbums = dynamoRepository.getAlbums(gallery);
        log.info("Retrieved {} albums for user: {}", allAlbums.size(), gallery);
        return allAlbums;
    }

    public Optional<AlbumResponse> getAlbum(String gallery, String albumId) {
        return dynamoRepository.getAlbum(gallery, albumId)
                .map(album -> {
                    // extract album and process photos.
                    List<Photo> photos = dynamoRepository.getPhotos(album.getAlbumId());
                    log.info("Found {} photos for album: {}", photos.size(), album.getAlbumId());

                    photos.forEach(photo ->
                            photo.setPreSignedUrl(s3Service.generatePresignedUrl(gallery + "/" + album.getName() + "/" + photo.getS3FileName()))
                    );

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
        Album album = Album.builder()
                .gallery(gallery)
                .albumId(albumId)
                .name(createAlbumRequest.getName())
                .date(LocalDateTime.now(clock))
                .pk("PHOTOGALLERY#" + gallery)
                .sk("ALBUM#" + albumId)
                .build();
        dynamoRepository.createAlbum(album);
        log.info("Successfully created album: {}", albumId);
        return album;
    }

}
