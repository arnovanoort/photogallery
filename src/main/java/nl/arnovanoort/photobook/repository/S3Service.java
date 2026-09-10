package nl.arnovanoort.photobook.repository;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

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
}
