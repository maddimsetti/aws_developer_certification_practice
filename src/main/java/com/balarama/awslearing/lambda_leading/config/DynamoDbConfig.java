package com.balarama.awslearing.lambda_leading.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class DynamoDbConfig {
	
	@Value("${aws.region}")
    private String awsRegion;
	
	@Value("${aws.iam.username}")
	private String iamUser;
	
	@Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(awsRegion))  // Change your region
                .credentialsProvider(ProfileCredentialsProvider.create(iamUser))
                .build();
    }
	
	@Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(ProfileCredentialsProvider.create(iamUser))
                .build();
    }
	
	@Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(ProfileCredentialsProvider.create(iamUser))
                .build(); // ✅ Automatically picks up credentials from env/profile
    }
	
	@Bean
	public SnsClient snsClient() {
		return SnsClient.builder().region(Region.of(awsRegion))
				.credentialsProvider(ProfileCredentialsProvider.create(iamUser))
				.build();
	}

	@Bean
	public SqsClient sqsClient() {
		return SqsClient.builder().region(Region.of(awsRegion))
				.credentialsProvider(ProfileCredentialsProvider.create(iamUser))
				.build();
	}
	
	@Bean
	public CloudWatchClient cloudWatchClient() {
		return CloudWatchClient.builder().region(Region.of(awsRegion))
				.credentialsProvider(ProfileCredentialsProvider.create(iamUser))
				.build(); 
	}
	
	@Bean
	public LambdaClient lambdaClient() {
		return LambdaClient.builder().region(Region.of(awsRegion))
				.credentialsProvider(ProfileCredentialsProvider.create(iamUser))
				.build();
	}

}
