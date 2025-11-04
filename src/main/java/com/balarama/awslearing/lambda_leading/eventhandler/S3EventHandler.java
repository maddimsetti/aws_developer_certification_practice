package com.balarama.awslearing.lambda_leading.eventhandler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.balarama.awslearing.lambda_leading.service.FileMetadataService;
import com.balarama.awslearing.lambda_leading.service.SnsService;
import com.balarama.awslearing.lambda_leading.service.SqsService;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.SqsClient;

public class S3EventHandler implements RequestHandler<S3Event, String> {
	
	private final FileMetadataService fileMetadataService;
	private final SnsService snsService;
	private final SqsService sqsService;

	// ✅ No-arg constructor required by AWS Lambda
    public S3EventHandler() {
        // Manually build DynamoDbClient (avoid Spring injection)
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.EU_WEST_1) // Lambda will set AWS_REGION
                .build();
        
        SnsClient snsClient = SnsClient.builder().region(Region.EU_WEST_1).build();
        SqsClient sqsClient = SqsClient.builder().region(Region.EU_WEST_1).build();
        this.fileMetadataService = new FileMetadataService(dynamoDbClient);
        this.snsService = new SnsService(snsClient);
        this.sqsService = new SqsService(sqsClient);        
    }


    @Override
    public String handleRequest(S3Event event, Context context) {
    	
    	// ----- Simulated failure for roll back testing ----
        String fail = System.getenv("FAIL_DEPLOY");
        if ("true".equalsIgnoreCase(fail)) 
        {
            throw new RuntimeException("Simulated failure for rollback test");
        }
        
        context.getLogger().log("🚀 Lambda triggered with records: " 
                + (event.getRecords() != null ? event.getRecords().size() : 0));

        for (S3EventNotificationRecord record : event.getRecords()) {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();
            long size = record.getS3().getObject().getSizeAsLong();

            context.getLogger().log("✅ File uploaded: " + key + " (" + size + " bytes) to bucket: " + bucket);
            
            fileMetadataService.saveFileMetadata(bucket, key, size);
            
            PublishResponse snsResponse = snsService.publish(bucket, key, size);
            
            context.getLogger().log("Published message to SNS. MessageId: " + snsResponse.messageId());
            
            //sqsService.sendMessage(bucket, key, size);
            
        }
        return "OK";
    }
}


