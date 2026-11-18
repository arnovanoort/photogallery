package nl.arnovanoort.photogallery.service;

import nl.arnovanoort.photogallery.dto.*;
import nl.arnovanoort.photogallery.model.Album;
import nl.arnovanoort.photogallery.repository.AlbumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static nl.arnovanoort.photogallery.ObjectMother.*;
import static nl.arnovanoort.photogallery.ObjectMother.aListOf2PhotosWithoutLink;
import static nl.arnovanoort.photogallery.ObjectMother.aListOf2Albums;
import static nl.arnovanoort.photogallery.ObjectMother.anAlbum;
import static nl.arnovanoort.photogallery.ObjectMother.anEmptyListOfAlbums;
import static nl.arnovanoort.photogallery.ObjectMother.testAlbumId;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlbumServiceTest {
    AlbumService albumService;

    @Mock
    private AlbumRepository mockAlbumRepository;
    @Mock
    private StorageService mockStorageService;


    private Album testAlbum;

    @BeforeEach
    void setUp() {
        albumService = new AlbumService(mockStorageService, mockAlbumRepository, clock);
        this.testAlbum = anAlbum();
    }

    @Test
    void albumService_getAllAlbums_success() throws  Exception{
        when(mockAlbumRepository.getAlbums(testGallery)).thenReturn(aListOf2Albums());

        // execute
        List<Album> result = albumService.getAlbums(testGallery);

        // assert
        assertEquals(result, aListOf2Albums());
    }

    @Test
    void albumService_getAllAlbums_success_for_emnpty_list() throws  Exception{
        when(mockAlbumRepository.getAlbums(testGallery)).thenReturn(anEmptyListOfAlbums());

        // execute
        List<Album> result = albumService.getAlbums(testGallery);

        // assert
        assertEquals(result.size(), 0);
    }

    @Test
    void albumService_getAlbum_album_Found() throws Exception {
        // prepare
        when(mockAlbumRepository.getAlbum(testGallery, testAlbumId)).thenReturn(Optional.ofNullable(testAlbum));
        when(mockAlbumRepository.getPhotos(testAlbumId)).thenReturn(aListOf2PhotosWithoutLink());
        when(mockStorageService.generatePublicURL(testGallery, anAlbum().getName(), aPhotoWithoutLink().getFilename())).thenReturn(publicLinkForAPhoto);
        when(mockStorageService.generatePublicURL(testGallery, anAlbum().getName(), anOtherPhotoWithoutLink().getFilename())).thenReturn(publicLinkForAnotherPhoto);

        // execute
        Optional<AlbumResponse> result = albumService.getAlbum(testGallery, testAlbumId);

        // assert
        assertEquals(anAlbum(), result.get().getAlbum());
        assertEquals(aListOf2Photos(), result.get().getPhotos());
    }

    @Test
    void albumService_getAlbum_not_existing_albumId() throws Exception {
        // prepare
        when(mockAlbumRepository.getAlbum(testGallery, testAlbumId)).thenReturn(Optional.empty());

        // execute
        Optional<AlbumResponse> result = albumService.getAlbum(testGallery, testAlbumId);

        // assert
        assertTrue(result.isEmpty());
    }

    @Test
    void albumService_getAlbum_album_Found_but_no_photos() throws Exception {
        // prepare
        when(mockAlbumRepository.getAlbum(testGallery, testAlbumId)).thenReturn(Optional.ofNullable(testAlbum));
        when(mockAlbumRepository.getPhotos(testAlbumId)).thenReturn(anEmptyListOfPhotosWithoutLink());

        // execute
        Optional<AlbumResponse> result = albumService.getAlbum(testGallery, testAlbumId);

        // assert
        assertEquals(anAlbum(), result.get().getAlbum());
        assertEquals(anEmptyListOfPhotosWithoutLink(), result.get().getPhotos());
    }

    @Test
    void albumService_create_album() throws Exception {
        // prepare
        CreateAlbumRequest createAlbumRequest = CreateAlbumRequest.builder()
                .name(anAlbum().getName())
                .albumId(testAlbumId)
                .build();

        // execute
        Album result = albumService.createAlbum(testGallery, createAlbumRequest);

        // assert
        assertEquals(anAlbum(), result);
    }

    @Test
    void albumService_create_album_without_albumId_should_generate_one() throws Exception {
        // prepare
        CreateAlbumRequest createAlbumRequest = CreateAlbumRequest.builder()
                .name(anAlbum().getName())
                .albumId(null) // --> set to null
                .build();

        // execute
        Album result = albumService.createAlbum(testGallery, createAlbumRequest);

        // assert
        assertNotNull(result.getAlbumId());
    }


    @Test
    void albumService_import_albums() throws Exception {
        // mock listAlbums call
        List<AlbumImport> imports = List.of(
                new AlbumImport(testAlbumName, List.of("foto1.jpg", "foto2.jpg")),
                new AlbumImport(testAnotherAlbumName, List.of("foto3.jpg"))
        );
        when(mockStorageService.listAlbums(testGallery)).thenReturn(imports);

        // execute
        ImportResponse result = albumService.importAlbums(testGallery);

        // assert: status and album names
        assertEquals(testGallery, result.getGalleryName());
        assertEquals(Arrays.asList(new String[]{testAlbumName, testAnotherAlbumName}), result.getAlbumNames());

        // assert: 2 calls to importAlbums
        verify(mockAlbumRepository, times(2)).createAlbumAndPhotos(any(Album.class), anyList());
    }


}
