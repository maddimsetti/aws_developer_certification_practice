package com.balarama.awslearing.lambda_leading.eventhandler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.balarama.awslearing.lambda_leading.service.FileMetadataService;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class S3EventHandler implements RequestHandler<S3Event, String> {
	
	private final FileMetadataService fileMetadataService;

	// ✅ No-arg constructor required by AWS Lambda
    public S3EventHandler() {
        // Manually build DynamoDbClient (avoid Spring injection)
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.EU_WEST_1) // Lambda will set AWS_REGION
                .build();

        this.fileMetadataService = new FileMetadataService(dynamoDbClient);
    }


    @Override
    public String handleRequest(S3Event event, Context context) {
        context.getLogger().log("🚀 Lambda triggered with records: " 
                + (event.getRecords() != null ? event.getRecords().size() : 0));

        for (S3EventNotificationRecord record : event.getRecords()) {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();
            long size = record.getS3().getObject().getSizeAsLong();

            context.getLogger().log("✅ File uploaded: " + key + " (" + size + " bytes) to bucket: " + bucket);
            fileMetadataService.saveFileMetadata(bucket, key, size);
        }
        return "OK";
    }
}


