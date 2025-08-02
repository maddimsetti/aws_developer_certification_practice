package com.balarama.awslearing.lambda_leading.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

@RestController
public class S3UploaderController {

	@Autowired
	private AmazonS3 amazonS3;

	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;

	@Autowired
	private AWSLambda awsLambda;
	
	@Value("${aws.lambda.function-name}")
	private String lambdaFunctionName;


	@PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestPart MultipartFile file) throws IOException {
		String key = "uploads/" + file.getOriginalFilename();
		amazonS3.putObject(bucketName, key, file.getInputStream(), new ObjectMetadata());

		// Create Lambda Request
		InvokeRequest invokeRequest = new InvokeRequest().withFunctionName(lambdaFunctionName)
				.withPayload("{ \"fileName\": \"" + key + "\" }");

		// Invoke Lambda
		awsLambda.invoke(invokeRequest);

		return ResponseEntity.ok("File uploaded and Lambda triggered");
	}
}
