package com.balarama.awslearing.lambda_leading.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RestController
@RequestMapping("/api/files")
public class S3UploaderController {
	
	private final S3Client s3Client;
	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;
		
	public S3UploaderController(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	@PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestPart MultipartFile file) throws IOException {
		String key = file.getOriginalFilename();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .acl("private") // Lambda will have access via trigger
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        return ResponseEntity.ok("File uploaded to S3: " + file.getOriginalFilename());
	}
}
