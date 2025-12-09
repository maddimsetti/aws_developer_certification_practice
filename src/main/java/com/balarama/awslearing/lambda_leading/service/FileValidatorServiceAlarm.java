package com.balarama.awslearing.lambda_leading.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.balarama.awslearing.lambda_leading.DTO.FileFetchResult;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.ComparisonOperator;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;

@Service
public class FileValidatorServiceAlarm {

	private final String BUCKET_NAME = "lambdas3-practice";
	private final String alarmName = "InvalidFileCountAlarmToS3";
	private final String namespace = "FileValidatorApp";
	private final String metricName = "InvalidFileCount";
	private final String METRIC_FILE_FOUND = "FileFoundMetric";
	private final String METRIC_FILE_NOT_FOUND = "FileFoundNotMetric";
	
	private CloudWatchClient cloudWatchClient;
	private SnsService snsService;
	private S3Client s3Client;

	public FileValidatorServiceAlarm(CloudWatchClient cloudWatchClient, SnsClient snsClient, S3Client s3Client) {
		this.cloudWatchClient = cloudWatchClient;
		this.snsService = new SnsService(snsClient);
		this.s3Client = s3Client;
	}
	
	public String uploadFile(String fileName, byte[] fileBytes) {
		try {
			s3Client.putObject(PutObjectRequest.builder()
					.bucket(BUCKET_NAME)
					.key(fileName)
					.acl("private") // Lambda
					.build(), 
				RequestBody.fromBytes(fileBytes));
			
			boolean valid = fileName.endsWith(".txt");
			String postMetricName = valid ? "ValidFileCount" : "InvalidFileCount";
			
			validateAndPushMetric(postMetricName);
			
			return valid ? "File is valid. Metric: " + postMetricName + " pushed. And uploaded to " + BUCKET_NAME
					: "Invalid file! Metric: " + postMetricName + " pushed And uploaded to " + BUCKET_NAME;
		} catch(Exception e) {
			throw new RuntimeException("Error while uploading the file: " + e.getMessage(), e);
		}
		
	}
	
	public FileFetchResult checkAndFetchFile(String fileName) {
		try {
			// 1️⃣ Check S3 existence using headObject
	        s3Client.headObject(builder -> builder
	                .bucket(BUCKET_NAME)
	                .key(fileName)
	        );
	        
	        // 2️⃣ File exists — fetch it
	        	byte[] fileBytes = s3Client.getObjectAsBytes(
		                builder -> builder.bucket(BUCKET_NAME).key(fileName)
		        ).asByteArray();
	        	
	        	validateAndPushMetric(METRIC_FILE_FOUND);
	        	return new FileFetchResult(
	                    true,
	                    fileBytes,
	                    "File found in bucket: " + fileName
	            );
	        
		} catch(NoSuchKeyException e) {
			validateAndPushMetric(METRIC_FILE_NOT_FOUND);
			return new FileFetchResult(
	                false,
	                null,
	                "File NOT found: " + fileName
	        );
		}
		
	}
	
	/**
     * Uploads and validates file name, sends CloudWatch metric if invalid.
     */
    private void validateAndPushMetric(String metricName) {
		try {
			// Push metric with value 1 per invalid file
			MetricDatum datum = 
					MetricDatum.builder()
								.metricName(metricName)
								.unit(StandardUnit.COUNT)
								.value(1.0)
								.timestamp(Instant.now()) // optional: align to current minute
								.dimensions(Dimension.builder().name("App").value("Validator").build())
								.build();

			PutMetricDataRequest request = 
					PutMetricDataRequest.builder()
										.namespace("FileValidatorApp")
										.metricData(datum)
										.build();

			cloudWatchClient.putMetricData(request);
			System.out.println("✅ Metric pushed: " + metricName);

			
		} catch (Exception e) {
			throw new RuntimeException("Error while validating and pushing metrci: " + e.getMessage(), e);
		}
    }
    
    /**
     * Fires when InvalidFileCount > 5 within 5 minutes (5 x 60s)
     */
    public String createInvalidFileAlarm(String email) {
        
        // 1) Create SNS topic for alerting
        String snsTopicArn = snsService.createTopic();
     		
     	//checking for subscription
     	snsService.subscribeEmail(email);
     		
        Dimension appDim = Dimension.builder()
                .name("App").value("Validator").build();

        // 🧠 New alarm configuration:
        // Fires when InvalidFileCount > 5 within 5 minutes (5 x 60s)
        PutMetricAlarmRequest alarmRequest = PutMetricAlarmRequest.builder()
                .alarmName(alarmName)
                .alarmDescription("Triggers when InvalidFileCount > 5 within 5 minutes in Dev environment")
                .namespace(namespace)
                .metricName(metricName)
                .dimensions(appDim)
                .statistic(Statistic.SUM)
                .period(300) // 5 minute
                .threshold(5.0)  // trigger when sum > 5 in 5-min period
                .evaluationPeriods(1) // only need 1 period of 5 min
                .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                .alarmActions(snsTopicArn)
                .build();

        cloudWatchClient.putMetricAlarm(alarmRequest);
        
        return "Alarm created for S3 Uploader: " + alarmName + ", SNS topic: " + snsTopicArn;
    }
}
