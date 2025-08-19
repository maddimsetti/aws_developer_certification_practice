package com.balarama.awslearing.lambda_leading.eventhandler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

public class S3EventHandler implements RequestHandler<S3Event, String> {

    @Override
    public String handleRequest(S3Event event, Context context) {
        context.getLogger().log("🚀 Lambda triggered with records: " 
                                + (event.getRecords() != null ? event.getRecords().size() : 0) + "\n");

        for (S3EventNotificationRecord record : event.getRecords()) {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();
            long size = record.getS3().getObject().getSizeAsLong(); // Sometimes returns 0 if not provided

            context.getLogger().log("✅ File uploaded: " + key + 
                    " (" + size + " bytes) to bucket: " + bucket + "\n");
        }
        return "OK";
    }
}


