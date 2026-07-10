package nl.arnovanoort.photobook;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import nl.arnovanoort.photobook.repository.DynamoRepository;
import nl.arnovanoort.photobook.repository.S3Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlbumHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public final static String PHOTOBOOK_NAME = "Arno"; // replace in next phase with requested photobook

    private final S3Service s3Service;
    private final ObjectMapper objectMapper = new ObjectMapper();
    protected DynamoRepository dynamoRepository;
    // Default constructor voor AWS Lambda
    public AlbumHandler() {
        this.s3Service = new S3Service();
    }

    // Constructor voor Unit Testing (Dependency Injection)
    public AlbumHandler(DynamoRepository dynamoRepository, S3Service s3Service) {
        this.dynamoRepository = dynamoRepository;
        this.s3Service = s3Service;
    }

    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String path = request.getPath();
        String httpMethod = request.getHttpMethod();

        if ("/albums".equals(path)) {
            if ("GET".equals(httpMethod)) {
                return handleGetAllAlbums();
            }else if("POST".equals(httpMethod)){
                AlbumRequest albumRequest = objectMapper.readValue(request.getBody(), AlbumRequest.class);
                return handleCreateAlbum(albumRequest);

            }
        } else if ("/album".equals(path)) {
            if ("GET".equals(httpMethod)) {
                return handleGetSingleAlbum(request);
            }
        }

        // Voor alle andere methoden of onbekende paden
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(405) // Method Not Allowed
                .withBody("Methode " + httpMethod + " niet toegestaan voor pad " + path);
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGetAllAlbums() {
        List<Album> allAlbums = dynamoRepository.getAlbums(PHOTOBOOK_NAME);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of(
                        "Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*"
                ))
                .withBody(objectMapper.writeValueAsString(allAlbums));
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleGetSingleAlbum(APIGatewayProxyRequestEvent request) {
        String albumId = "1"; // Default waarde
        if (request.getQueryStringParameters() != null && request.getQueryStringParameters().containsKey("albumId")) {
            albumId = request.getQueryStringParameters().get("albumId");
        }

        Album album = dynamoRepository.getAlbum("ALBUM#" + albumId);

        if (album == null) {
            return create404Response("Album niet gevonden");
        }

        List<Photo> photos = dynamoRepository.getPhotos("ALBUM#" + albumId);

        photos.forEach(photo -> photo.setPreSignedUrl(s3Service.generatePresignedUrl(photo.getS3FileName())));

        AlbumResponse responseBody = AlbumResponse.builder()
                .album(album)
                .photos(photos)
                .build();
        return create200Response(responseBody);
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent handleCreateAlbum(AlbumRequest albumRequest) {
        String albumId = String.valueOf(System.currentTimeMillis());

        Album album = Album.builder()
                .photobook(PHOTOBOOK_NAME)
                .albumId(albumId)
                .metadata("METADATA")
                .naam(albumRequest.getNaam())
                .datum(albumRequest.getDatum())
                .build();
        dynamoRepository.createAlbum(album);

        return create201Response(album);
    }


    @SneakyThrows
    private APIGatewayProxyResponseEvent create200Response(Object responseBody){
        return createResponse(responseBody, 200);
    }
    @SneakyThrows
    private APIGatewayProxyResponseEvent create201Response(Object responseBody){
        return createResponse(responseBody, 201);
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent create404Response(Object responseBody){
        return createResponse(responseBody, 404);
    }

    @SneakyThrows
    private APIGatewayProxyResponseEvent createResponse(Object responseBody, int statusCode){
        String body = responseBody instanceof String ? (String) responseBody : objectMapper.writeValueAsString(responseBody);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of(
                        "Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*"
                ))
                .withBody(body);

    }
}
