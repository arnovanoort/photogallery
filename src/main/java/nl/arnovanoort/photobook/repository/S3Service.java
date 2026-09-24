package nl.arnovanoort.photobook.repository;

import lombok.extern.slf4j.Slf4j;
import nl.arnovanoort.photobook.dto.AlbumImport;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class S3Service {

    private final String bucketName;
    private final S3Presigner s3Presigner;

    public S3Service(String bucketName, S3Presigner s3Presigner) {
        this.bucketName = bucketName;
        this.s3Presigner = s3Presigner;
    }

    public S3Service() {
        this.bucketName = System.getenv("BUCKET_NAME") != null ? System.getenv("BUCKET_NAME") : "photo-bucket";
        this.s3Presigner = S3Presigner.create();
    }

    protected S3Service(String bucketName) {
        this.bucketName = bucketName;
        this.s3Presigner = S3Presigner.create();
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

    public List<AlbumImport> listBuckets() {
        log.info("fetching folders in bucket: " + bucketName);
        List<AlbumImport> albums = new ArrayList<AlbumImport>();
        S3Client s3 = S3Client.builder()
                .region(Region.EU_WEST_1)
                .build();

        // Delimiter "/" zorgt ervoor dat S3 stopt bij de eerste submap
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .delimiter("/")
                // optioneel: .prefix("albums/") als je in een specifieke submap wilt zoeken
                .build();

        ListObjectsV2Response response = s3.listObjectsV2(request);

        System.out.println("Gevonden mappen in bucket '" + bucketName + "':");

        for (CommonPrefix commonPrefix : response.commonPrefixes()) {
            albums.add(
                new AlbumImport(
                    commonPrefix.prefix(),
                    listPhotosInAlbum(commonPrefix.prefix())
                )
            );
            log.info("adding " + albums.getLast());
        }
        return albums;
    }

    public List<String> listPhotosInAlbum(String albumPrefix) {
        log.info("fetching photos in album: " + albumPrefix);
        List<String> photos = new ArrayList<>();
        S3Client s3 = S3Client.builder()
                .region(Region.EU_WEST_1)
                .build();

        // Geen delimiter, wel prefix om alle objecten in de folder te krijgen
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(albumPrefix)
                .build();

        ListObjectsV2Response response = s3.listObjectsV2(request);

        for (var s3Object : response.contents()) {
            // Sla de folder zelf over (bijv. "album1/" zonder bestandsnaam)
            if (!s3Object.key().equals(albumPrefix)) {
                photos.add(s3Object.key());
                log.info(" - " + s3Object.key());
            }
        }
        return photos;
    }

}
