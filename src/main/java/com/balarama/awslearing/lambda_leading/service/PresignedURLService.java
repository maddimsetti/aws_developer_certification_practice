package com.balarama.awslearing.lambda_leading.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
public class PresignedURLService {
	
	private final S3Presigner s3Presigner;
	
	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;
	
	@Value("${aws.region}")
	private String awsRegion;
	
	@Value("${aws.access.id}")
	private String accessID;
	
	@Value("${aws.secret.token}")
	private String secretKey;
	
	@Value("${aws.service}")
	private String awsService;

	public PresignedURLService(S3Presigner s3Presigner) {
		this.s3Presigner = s3Presigner;
	}
	
	public String generateUploadURL(String fileName, String contentType, int expiryTime, long maxSizeBytes) {
		
		// ✅ allow only safe content types
	    if (!(contentType.equals("application/pdf") ||
	          contentType.equals("image/jpeg") ||
	          contentType.equals("image/png"))) {
	        throw new IllegalArgumentException("Unsupported content type: " + contentType);
	    }

	    // ✅ max size validation (app-side)
	    if (maxSizeBytes > 10 * 1024 * 1024) { // 10 MB hard limit
	        throw new IllegalArgumentException("File too large. Max allowed = 10 MB");
	    }
	    
		PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(bucketName).key(fileName).contentType(contentType).build();
		
		PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(expiryTime))
                .putObjectRequest(objectRequest));
		
		return presignedRequest.url().toString();
	}
	
	public String generateDownloadURL(String fileName, int expirationTime) {
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                // 👇 Add Content-Disposition so browser forces download
                .responseContentDisposition("attachment; filename=\"" + fileName + "\"")
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofMinutes(expirationTime))
                .getObjectRequest(getObjectRequest));

        return presignedRequest.url().toString();
	}

	public String generatePostUploadURL(String fileName, String contentType, int expiryTime, long maxSizeBytes) throws Exception {
		// allow only certain content types
        if (!(contentType.equals("application/pdf")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }

        // expiration
        Instant expiration = Instant.now().plus(Duration.ofMinutes(expiryTime));

        // conditions
        List<Object> conditions = new ArrayList<>();
        conditions.add(Collections.singletonMap("bucket", bucketName));
        conditions.add(Collections.singletonMap("key", fileName));
        conditions.add(Collections.singletonMap("Content-Type", contentType));
        conditions.add(Arrays.asList("content-length-range", 0, maxSizeBytes));

        Map<String, Object> policyDoc = new HashMap<>();
        policyDoc.put("expiration", expiration.toString());
        policyDoc.put("conditions", conditions);

        String policy = Base64.getEncoder().encodeToString(
                new ObjectMapper().writeValueAsBytes(policyDoc)
        );

        // Generate signature (you’ll need helper methods below)
        String amzDate = getAmzDate();
        String credentialScope = getCredentialScope(amzDate);
        String signature = signPolicy(policy, amzDate, credentialScope);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("url", "https://" + bucketName + ".s3." + awsRegion + ".amazonaws.com");
        response.put("key", fileName);
        response.put("Content-Type", contentType);
        response.put("policy", policy);
        response.put("x-amz-algorithm", "AWS4-HMAC-SHA256");
        response.put("x-amz-credential", accessID + "/" + credentialScope);
        response.put("x-amz-date", amzDate);
        response.put("x-amz-signature", signature);

        return response.toString();
	}

    private String getAmzDate() {
        return DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }

    private String getCredentialScope(String amzDate) {
        String dateStamp = amzDate.substring(0, 8);
        return dateStamp + "/" + awsRegion + "/"+ awsService+ "/aws4_request";
    }

    private String signPolicy(String policy, String amzDate, String credentialScope) throws Exception {
        String dateStamp = amzDate.substring(0, 8);
        byte[] signingKey = getSignatureKey(secretKey, dateStamp, awsRegion, "s3");
        byte[] signatureBytes = HmacSHA256(signingKey, policy);
        return bytesToHex(signatureBytes);
    }

    // AWS4 signing helpers
    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kDate = HmacSHA256(("AWS4" + key).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = HmacSHA256(kDate, regionName);
        byte[] kService = HmacSHA256(kRegion, serviceName);
        return HmacSHA256(kService, "aws4_request");
    }

    private byte[] HmacSHA256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

}
