package org.example;

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
public class Album {
    private String albumId;
    private String metadata;
    private String naam;
    private String locatie;
    private String datum;

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
        return "METADATA";
    }

    public void setSk(String sk) {
        // SK is fixed for album metadata
    }
}
