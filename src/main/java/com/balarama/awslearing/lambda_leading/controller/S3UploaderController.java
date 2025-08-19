package com.balarama.awslearing.lambda_leading.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RestController
public class S3UploaderController {

	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;
	
	@Value("${aws.region}")
	private Region region;


	@PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestPart MultipartFile file) throws IOException {
		S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

		String key = file.getOriginalFilename();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .acl("private") // Lambda will have access via trigger
                .build();

        s3.putObject(request, RequestBody.fromBytes(file.getBytes()));

        return ResponseEntity.ok("File uploaded to S3: " + key);
	}
}
