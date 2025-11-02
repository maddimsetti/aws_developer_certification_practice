package com.balarama.awslearing.lambda_leading.eventhandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.balarama.awslearing.lambda_leading.service.FileValidatorServiceAlarm;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;

public class UploaderLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final FileValidatorServiceAlarm validatorServiceAlarm;

    // ✅ No-arg constructor required by AWS Lambda
    public UploaderLambdaHandler() {
        CloudWatchClient cloudWatchClient = CloudWatchClient.builder()
                .region(Region.EU_WEST_1)
                .build();

        SnsClient snsClient = SnsClient.builder()
                .region(Region.EU_WEST_1)
                .build();

        S3Client s3Client = S3Client.builder()
                .region(Region.EU_WEST_1)
                .build();

        this.validatorServiceAlarm = new FileValidatorServiceAlarm(cloudWatchClient, snsClient, s3Client);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        context.getLogger().log("📩 Received upload event");

        try {
            // 1️⃣ Extract filename
            String fileName = extractFileName(event);
            context.getLogger().log("🧾 Filename received: " + fileName);

            // 2️⃣ Decode body safely
            byte[] fileBytes = decodeFileBody(event);

            // 3️⃣ Validate file content strongly
            validateFileContent(fileBytes, fileName);

            // 4️⃣ Pass to service class for upload + metrics + SNS
            String message = validatorServiceAlarm.validateAndPushMetric(fileName, fileBytes);

            context.getLogger().log("✅ File uploaded successfully: " + message);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("✅ File uploaded successfully to S3: " + fileName);

        } catch (IllegalArgumentException e) {
            context.getLogger().log("⚠️ Validation failed: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Validation error: " + e.getMessage());
        } catch (Exception e) {
            context.getLogger().log("❌ Upload failed: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error uploading file: " + e.getMessage());
        }
    }

    // ✅ Extract filename safely (from query or multipart)
    private String extractFileName(APIGatewayProxyRequestEvent event) throws IOException {
        String fileName = "uploaded-file.txt";

        Map<String, String> params = event.getQueryStringParameters();
        if (params != null) {
            if (params.containsKey("filename")) fileName = params.get("filename");
            else if (params.containsKey("fileName")) fileName = params.get("fileName");
        }

        // Fallback for multipart/form-data
        if (event.getHeaders() != null && event.getHeaders().containsKey("Content-Type")) {
            String contentType = event.getHeaders().get("Content-Type");
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                try {
                    String decodedBody = new String(Base64.getDecoder().decode(event.getBody()), StandardCharsets.UTF_8);
                    Matcher matcher = Pattern.compile("filename=\"([^\"]+)\"").matcher(decodedBody);
                    if (matcher.find()) {
                        fileName = matcher.group(1);
                    }
                } catch (Exception ignore) {
                    // safe fallback
                }
            }
        }

        if (!fileName.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid filename format: " + fileName);
        }

        return fileName;
    }

    // ✅ Decode body safely
    private byte[] decodeFileBody(APIGatewayProxyRequestEvent event) {
        if (event.getBody() == null || event.getBody().isEmpty()) {
            throw new IllegalArgumentException("Empty file content.");
        }

        return Boolean.TRUE.equals(event.getIsBase64Encoded())
                ? Base64.getDecoder().decode(event.getBody())
                : event.getBody().getBytes(StandardCharsets.UTF_8);
    }

    // ✅ Strong validation (content-based, not just extension)
    private void validateFileContent(byte[] fileBytes, String fileName) {
        if (fileBytes.length == 0) {
            throw new IllegalArgumentException("File is empty.");
        }

        // Detect common file signatures (magic numbers)
        if (isValidFileSignature(fileBytes)) return; // good file

        // Last check: plain readable text file
        if (isTextFile(fileBytes)) return;

        throw new IllegalArgumentException("Invalid or corrupted file content.");
    }

    // ✅ Check for valid binary signatures (magic numbers)
    private boolean isValidFileSignature(byte[] bytes) {
        return startsWith(bytes, new byte[]{0x25, 0x50, 0x44, 0x46}) || // PDF
               startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}) || // JPG
               startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}) || // PNG
               startsWith(bytes, new byte[]{0x47, 0x49, 0x46, 0x38}) || // GIF
               startsWith(bytes, new byte[]{0x50, 0x4B, 0x03, 0x04}); // ZIP/DOCX
    }

    // ✅ Check if it's plain readable text
    private boolean isTextFile(byte[] data) {
        int readable = 0;
        int sample = Math.min(200, data.length);

        for (int i = 0; i < sample; i++) {
            byte b = data[i];
            if ((b >= 32 && b <= 126) || b == '\n' || b == '\r' || b == '\t') {
                readable++;
            }
        }

        return ((float) readable / sample) > 0.85;
    }

    // ✅ Helper to check magic bytes
    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
