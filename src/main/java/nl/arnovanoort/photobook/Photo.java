package nl.arnovanoort.photobook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Photo {
    private String albumId;
    private String photoId;
    private String titel;
    private String s3FileName;
    private String datum;
    // Tijdelijk veld voor de Pre-signed URL
    private String preSignedUrl;

    @DynamoDbPartitionKey
    public String getPk() {
        return "ALBUM#" + albumId;
    }

    public void setPk(String pk) {
        if (pk != null && pk.startsWith("ALBUM#")) {
            this.albumId = pk.substring(6);
        }
    }

    @DynamoDbSortKey
    public String getSk() {
        return "PHOTO#" + photoId;
    }

    public void setSk(String sk) {
        if (sk != null && sk.startsWith("PHOTO#")) {
            this.photoId = sk.substring(6);
        }
    }
}
