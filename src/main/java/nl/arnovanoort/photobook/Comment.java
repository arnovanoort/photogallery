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
public class Comment {
    private String photoId;
    private String commentId;
    private String naam;
    private String tekst;
    private String datum;

    @DynamoDbPartitionKey
    public String getPk() {
        return "PHOTO#" + photoId;
    }

    public void setPk(String pk) {
        if (pk != null && pk.startsWith("PHOTO#")) {
            this.photoId = pk.substring(6);
        }
    }

    @DynamoDbSortKey
    public String getSk() {
        return "COMMENT#" + commentId;
    }

    public void setSk(String sk) {
        if (sk != null && sk.startsWith("COMMENT#")) {
            this.commentId = sk.substring(8);
        }
    }
}
