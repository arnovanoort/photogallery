package nl.arnovanoort.photogallery.service;

import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photogallery.dto.AlbumImport;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class S3Service {

    private final String bucketName;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    public S3Service(String bucketName, S3Client s3Client) {
        this.bucketName = bucketName;
        this.s3Presigner = S3Presigner.create();
        this.s3Client = s3Client;
    }

    public S3Service() {
        String awsRegion = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION")  : "eu-west-1";
        this.bucketName = System.getenv("BUCKET_NAME") != null ? System.getenv("BUCKET_NAME") : "photogallery-data";
        this.s3Presigner = S3Presigner.create();
        this.s3Client = createS3Client(awsRegion);
    }

    private S3Client createS3Client(String awsRegion) {
        return S3Client.builder().region(Region.of(awsRegion)).build();
    }

    public String generatePresignedUrl(String s3FileName) {
        if (s3FileName == null || s3FileName.isEmpty()) return null;

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3FileName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for file '{}' in bucket '{}'", s3FileName, bucketName, e);
            return null;
        }
    }

    public List<AlbumImport> listAlbums(String galleryName) {
        log.info("fetching folders in bucket: " + bucketName);
        List<AlbumImport> albums = new ArrayList<AlbumImport>();

        String galleryPrefix = galleryName.endsWith("/") ? galleryName : galleryName + "/";

        // Delimiter "/" ensures S3 stops at first subfolder
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .delimiter("/")
                .prefix(galleryPrefix)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        for (CommonPrefix commonPrefix : response.commonPrefixes()) {
            String fullAlbumPrefix = commonPrefix.prefix(); // e.g. "citytrips_gallery/london/"

            // 2. get albumname ("london")
            String cleanAlbumName = fullAlbumPrefix
                    .substring(galleryPrefix.length())
                    .replace("/", "");

            albums.add(
                new AlbumImport(
                        cleanAlbumName,                     // "london"
                        listPhotosInAlbum(fullAlbumPrefix)  // searches with "citytrips_gallery/london/"
                )
            );
            log.info("adding " + commonPrefix + " to " + albums.getLast());
        }
        return albums;
    }

    public List<String> listPhotosInAlbum(String albumPrefix) {
        log.info("fetching photos in album: " + albumPrefix);
        List<String> photos = new ArrayList<>();

        log.info("Reading bucket: " + bucketName);
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(albumPrefix)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        for (var s3Object : response.contents()) {
            // fetch filename from s3 path
            if (!s3Object.key().equals(albumPrefix) && s3Object.key().split("/").length > 1) {
                String filename = s3Object.key().split("/")[2];
                photos.add(filename);
                log.info("adding file " + filename + " to album " + albumPrefix);
            }
        }
        return photos;
    }

    public void uploadPhoto(String testGalleryCitytrips, String album,  String filename, byte[] bytes) {
        String key = testGalleryCitytrips + "/" + album + "/" + filename;
        log.info("uploading file " + key);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromBytes(bytes)
        )   ;
    }
}
