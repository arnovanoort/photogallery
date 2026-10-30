package nl.arnovanoort.photogallery.service;

import nl.arnovanoort.photogallery.dto.AlbumImport;
import nl.arnovanoort.photogallery.dto.AlbumRequest;
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

    public ImportResponse importAlbums(String gallery) {
        List<AlbumImport> albums = s3Service.listAlbums(gallery);
        albums.forEach(importAlbum -> {
            log.info("Importing album: {} for gallery {}", importAlbum, gallery);
            dynamoRepository.importAlbums(importAlbum, gallery);
        });
        return  ImportResponse.builder()
                .galleryName(gallery)
                .nrImportedAlbums(albums.size())
                .build();
    }

    public List<Album> getAlbums(String gallery){
        log.info("Fetching all albums for user: {}", gallery);
        List<Album> allAlbums = dynamoRepository.getAlbums(gallery);
        log.info("Retrieved {} albums for user: {}", allAlbums.size(), gallery);
        return allAlbums;
    }

    public Optional<AlbumResponse> getAlbum(String gallery, String albumId) {
        return Optional.ofNullable(dynamoRepository.getAlbum(gallery, albumId))
                .map(album -> {
                    // extract album and process photos.
                    List<Photo> photos = dynamoRepository.getPhotos("ALBUM#" + album.getAlbumId());
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

    public Album createAlbum(String gallery, AlbumRequest albumRequest) {
        String albumId = Optional.ofNullable(albumRequest.getAlbumId())
                .orElseGet(() -> java.util.UUID.randomUUID().toString());

        log.info("Creating album: name='{}', albumId='{}' for user: {}", albumRequest.getName(), albumId, gallery);

        Album album = Album.builder()
                .gallery(gallery)
                .albumId(albumId)
                .name(albumRequest.getName())
                .date(LocalDateTime.now(clock))
                .pk("PHOTOGALLERY#" + gallery)
                .sk("ALBUM#" + albumId)
                .build();
        dynamoRepository.createAlbum(album);
        log.info("Successfully created album: {}", albumId);
        return album;
    }

}
