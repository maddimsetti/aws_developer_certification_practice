package com.balarama.awslearing.lambda_leading.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class DynamoDbConfig {
	
	@Value("${aws.region}")
    private String awsRegion;
	
	@Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(awsRegion))  // Change your region
                .build();
    }
	
	@Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .build();
    }
	
	@Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(awsRegion))
                .build(); // ✅ Automatically picks up credentials from env/profile
    }
	
	@Bean
	public SnsClient snsClient() {
		return SnsClient.builder().region(Region.of(awsRegion)).build();
	}

}
