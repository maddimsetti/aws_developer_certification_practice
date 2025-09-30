package com.balarama.awslearing.lambda_leading.eventhandler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

public class SqsConsumerHandler implements RequestHandler<SQSEvent, Void> {

	@Override
	public Void handleRequest(SQSEvent event, Context context) {
		for(SQSEvent.SQSMessage message : event.getRecords()) {
			// parse message body, then process (e.g., write to DynamoDB)
            context.getLogger().log("Processing messageId=" + message.getMessageId() + " body=" + message.getBody());
            // if you throw here -> Lambda execution fails and message remains in queue; after retries it may go to DLQ.
			
			// Force failure for testing
	        //throw new RuntimeException("Simulated failure for DLQ testing");
		}
		return null;
	}

}
