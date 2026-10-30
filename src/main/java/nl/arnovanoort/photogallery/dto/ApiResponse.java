package nl.arnovanoort.photogallery.dto;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
@Slf4j
public class ApiResponse {

    private ApiResponse() {
        // Utility class
    }
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public static APIGatewayV2HTTPResponse ok(Object responseBody) {
        return createResponse(responseBody, 200);
    }

    public static  APIGatewayV2HTTPResponse created(Object responseBody) {
        return createResponse(responseBody, 201);
    }

    public static  APIGatewayV2HTTPResponse badRequest(String message) {
        return error(message, 400);
    }

    public static  APIGatewayV2HTTPResponse notFound(String message) {
        return error(message, 404);
    }

    public static  APIGatewayV2HTTPResponse methodNotAllowed(String message) {
        return error(message, 405);
    }

    public static APIGatewayV2HTTPResponse error(String message, int statusCode) {
        return createResponse(new ErrorResponse(message, statusCode), statusCode);
    }

    private static  APIGatewayV2HTTPResponse createResponse(Object responseBody, int statusCode) {
        String body;
        try {
            body = objectMapper.writeValueAsString(responseBody);
        } catch (Exception e) {
            log.error("Failed to serialize response body", e);
            body = "{\"message\":\"Internal Server Error\",\"status\":500}";
            statusCode = 500;
        }

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(Map.of(
                        "Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*"
                ))
                .withBody(body)
                .build();
    }

}
