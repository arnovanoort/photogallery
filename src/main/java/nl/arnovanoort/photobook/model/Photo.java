package nl.arnovanoort.photobook.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Photo {
    private String albumId;
    private String photoId;
    private String title;
    private String s3FileName;
    private String date;
    // Tijdelijk veld voor de Pre-signed URL
    private String preSignedUrl;

    private String pk;
    private String sk;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() {
        if (pk == null) {
            return "ALBUM#" + albumId;
        }
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
        if (pk != null && pk.startsWith("ALBUM#")) {
            this.albumId = pk.substring(6);
        }
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("sk")
    public String getSk() {
        if (sk == null) {
            return "PHOTO#" + photoId;
        }
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
        if (sk != null && sk.startsWith("PHOTO#")) {
            this.photoId = sk.substring(6);
        }
    }
    @DynamoDbIgnore
    public void setPreSignedUrl(String preSignedUrl) {
        this.preSignedUrl = preSignedUrl;
    }
}

