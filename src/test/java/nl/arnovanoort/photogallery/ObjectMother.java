package nl.arnovanoort.photogallery;

import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.model.Photo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class ObjectMother {
    public static String testAlbumName = "Test album name";
    public static String testAlbumId   = UUID.randomUUID().toString();
    public static String testAnotherAlbumName = "another album name";
    public static String testAnotherAlbumId   = UUID.randomUUID().toString();
    private static Album anAlbum;
    private static Album anOtherAlbum;
    private static Photo aPhoto;
    private static Photo anOtherPhoto;
    private static String aPhotoId = UUID.randomUUID().toString();
    private static String anOtherPhotoId = UUID.randomUUID().toString();
    public  static Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    public static Album anAlbum() {
        if(anAlbum == null) {
            return Album.builder()
                    .gallery(AlbumHandler.DEFAULT_GALLERY)
                    .albumId(testAlbumId)
                    .name(testAlbumName)
                    .date(LocalDateTime.now(clock))
                    .pk("PHOTOGALLERY#" + AlbumHandler.DEFAULT_GALLERY)
                    .sk("ALBUM#" + testAlbumId)
                    .build();
        }
        return anAlbum;
    }

    public static Album anOtherAlbum() {
        if(anOtherAlbum == null){
            return Album.builder()
                    .gallery(AlbumHandler.DEFAULT_GALLERY)
                    .albumId(testAnotherAlbumId)
                    .name(testAnotherAlbumName)
                    .pk("PHOTOGALLERY#" + AlbumHandler.DEFAULT_GALLERY)
                    .sk("ALBUM#" + testAnotherAlbumId)
                    .build();

        }
        return anOtherAlbum;
    }

    public static List<Album> alistOf2Albums() {
        return List.of(anAlbum(), anOtherAlbum());
    }
    public static List<Album> anEmptyListOfAlbums() {
        return List.of();
    }

    public static Photo aPhoto(){
        if(aPhoto == null){
            aPhoto = Photo.builder()
                    .albumId(testAlbumId)
                    .photoId(aPhotoId)
                    .title("Test photo")
                    .s3FileName("testPhotoS3Filename")
                    .date(LocalDateTime.now(clock).toString())
                    .preSignedUrl("test photo presigned url")
                    .pk("ALBUM#" + testAlbumId)
                    .sk("PHOTO#" + aPhotoId)
                    .build();
        }
        return aPhoto;
    }

    public static Photo anOtherPhoto(){
        if(anOtherPhoto == null){
            anOtherPhoto = Photo.builder()
                    .albumId(testAlbumId)
                    .photoId(anOtherPhotoId)
                    .title("another photo")
                    .s3FileName("anotherPhotoS3Filename.png")
                    .date(Instant.now().toString())
                    .preSignedUrl("another photo presigned url")
                    .pk("ALBUM#" + testAlbumId)
                    .sk("PHOTO#" + anOtherPhotoId)
                    .build();
        }
        return anOtherPhoto;
    }

    public static List<Photo> aListOf2Photos() {
        return List.of(aPhoto(), anOtherPhoto());
    }
}
