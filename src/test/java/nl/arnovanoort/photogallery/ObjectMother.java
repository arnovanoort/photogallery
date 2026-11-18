package nl.arnovanoort.photogallery;

import nl.arnovanoort.photogallery.dto.AlbumResponse;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.model.PhotoWithoutLink;
import nl.arnovanoort.photogallery.model.Photo;
import nl.arnovanoort.photogallery.repository.dynamo.DynamoAlbum;

import java.time.*;
import java.util.List;
import java.util.UUID;

public class ObjectMother {
    public final static String testGallery = "demo";

    private static Album anAlbum;
    private static Album anOtherAlbum;
    public static String testAlbumName = "Test album name";
    public static String testAlbumId   = UUID.randomUUID().toString();
    public static String testAnotherAlbumName = "another album name";
    public static String testAnotherAlbumId   = UUID.randomUUID().toString();

    private static PhotoWithoutLink aPhoto;
    private static PhotoWithoutLink anOtherPhoto;
    public static String publicLinkForAPhoto = "public link for a photo";
    public static String publicLinkForAnotherPhoto = "public link for an other photo";
    private static PhotoWithoutLink aPhotoWithoutLink;
    private static PhotoWithoutLink anOtherPhotoWithoutLink;
    private static String aPhotoId = UUID.randomUUID().toString();
    private static String anOtherPhotoId = UUID.randomUUID().toString();
    public  static Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private static LocalDate anAlbumDate = LocalDate.now(clock);

    // Helpers for albums

    public static Album anAlbum() {
        if(anAlbum == null) {
            anAlbum = Album.builder()
                    .gallery(testGallery)
                    .albumId(testAlbumId)
                    .name(testAlbumName)
                    .date(anAlbumDate)
                    .build();
        }
        return anAlbum;
    }

    public static Album anOtherAlbum() {
        if(anOtherAlbum == null){
            anOtherAlbum = Album.builder()
                    .gallery(testGallery)
                    .albumId(testAnotherAlbumId)
                    .name(testAnotherAlbumName)
                    .date(anAlbumDate)
                    .build();

        }
        return anOtherAlbum;
    }

    public static List<Album> aListOf2Albums() {
        return List.of(anAlbum(), anOtherAlbum());
    }
    public static List<Album> anEmptyListOfAlbums() {
        return List.of();
    }

    // Helpers for Photo

    public static PhotoWithoutLink aPhotoWithoutLink(){
        if(aPhotoWithoutLink == null){
            aPhotoWithoutLink = PhotoWithoutLink.builder()
                    .albumId(testAlbumId)
                    .photoId(aPhotoId)
                    .filename("testPhotoFilename")
                    .date(LocalDateTime.now(clock))
//                    .publicLink("test photo presigned url")
                    .build();
        }
        return aPhotoWithoutLink;
    }

    public static PhotoWithoutLink anOtherPhotoWithoutLink(){
        if(anOtherPhotoWithoutLink == null){
            anOtherPhotoWithoutLink = PhotoWithoutLink.builder()
                    .albumId(testAlbumId)
                    .photoId(anOtherPhotoId)
                    .filename("anotherTestPhotoFilename.png")
                    .date(LocalDateTime.now(clock))
                    .build();
        }
        return anOtherPhotoWithoutLink;
    }

    // Helpers for PhotoWithoutLink

    public static Photo aPhoto(){
        return aPhotoWithoutLink().toPhoto(publicLinkForAPhoto);
    }

    public static Photo anOtherPhoto(){
        return anOtherPhotoWithoutLink().toPhoto(publicLinkForAnotherPhoto);
    }

    public static List<Photo> aListOf2Photos() { return List.of(aPhoto(), anOtherPhoto()); }
    public static List<PhotoWithoutLink> aListOf2PhotosWithoutLink() { return List.of(aPhotoWithoutLink(), anOtherPhotoWithoutLink()); }
    public static List<PhotoWithoutLink> anEmptyListOfPhotosWithoutLink() {
        return List.of();
    }

    public static AlbumResponse albumResponseWith2Photos(){
      return AlbumResponse
              .builder()
              .album(anAlbum())
              .photos(aListOf2Photos())
              .build();
    }

    // Helpers for DynamoAlbum

    public static DynamoAlbum aDynamoAlbum = DynamoAlbum.builder().gallery(testGallery).albumId(testAlbumId).name(testAlbumName).date(LocalDate.now(clock)).build();
    public static DynamoAlbum anotherDynamoAlbum = DynamoAlbum.builder().gallery(testGallery).albumId(testAnotherAlbumId).name(testAnotherAlbumName).date(LocalDate.now(clock)).build();
    public static List<DynamoAlbum> dynamoAlbums = List.of(aDynamoAlbum, anotherDynamoAlbum);

}
