package com.balarama.awslearing.lambda_leading.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

@Service
public class SqsService {
	
	private static final String MAINQUEUENAME = "file-upload-queue";
	private static final String DLQNAME = "file-upload-dlq";
	private final SqsClient sqsClient;
	
	private String mainQueueURL;
	private String dlqURL;
	
	public SqsService(SqsClient sqsClient) {
		this.sqsClient = sqsClient;
	}
	
	/**
     * Initialize queues (create if not present, attach DLQ).
     */
	public String setupQueues() {
		// Create/get DLQ
		dlqURL = sqsClient.createQueue(CreateQueueRequest.builder().queueName(DLQNAME).build()).queueUrl();
		
		String dlqArn = getQueueArn(dlqURL);
		
		mainQueueURL = sqsClient.createQueue(CreateQueueRequest.builder().queueName(MAINQUEUENAME).build()).queueUrl();
		
		// Attach DLQ (if not already attached)
        String mainQueueArn = getQueueArn(mainQueueURL);
        attachDLQ(mainQueueURL, dlqArn);
        System.out.println("✅ Main Queue: " + MAINQUEUENAME + " [" + mainQueueArn + "]");
        System.out.println("✅ DLQ: " + DLQNAME + " [" + dlqArn + "]");
        
        return mainQueueArn;
	}
	
	/**
     * Publish a message to the main SQS queue.
     */
    public void sendMessage(String bucketName, String fileName, long filesize) {
    	String messageBody = "📂 File uploaded successfully in SQS: " + fileName + " (" + filesize + " bytes) in bucket: " + bucketName;
        if (mainQueueURL == null) setupQueues(); // lazy init
        sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(mainQueueURL)
                        .messageBody(messageBody)
                        .build());
        System.out.println("📨 Sent message successfully to the SQS: " + messageBody);
    }
	
	/**
     * Read messages from the main SQS queue.
     */
	public List<String> receiveMessagefromSQS() {
			if(mainQueueURL == null) setupQueues();  // lazy
			ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
														.queueUrl(mainQueueURL)
														.maxNumberOfMessages(5)
														.waitTimeSeconds(10)
														.build());
			
			List<Message> messages = response.messages();
			if(messages.isEmpty()) {
				System.out.println("ℹ️ No messages found.");
				return List.of(); // return empty list instead of null (cleaner for JSON)
			}
			
			// Extract message bodies
		    List<String> messageBodies = messages.stream()
		            .map(Message::body)
		            .toList();
			
		    // Delete messages after extracting bodies
			for (Message msg : messages) {
	            System.out.println("📥 Received: " + msg.body());
	            // After processing, delete it
	            deleteMessage(mainQueueURL, msg);
	            System.out.println("Deleted the messages from the SQS");
	        }
			
			return messageBodies;
	}
	
	public List<String> receiveDlqMessages() {
		if(dlqURL == null) setupQueues();  // lazy
		try {
			// Poll the DLQ
            ReceiveMessageResponse responseDlq = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(dlqURL)
                    .maxNumberOfMessages(5) // batch up to 10
                    .waitTimeSeconds(10)    // long polling
                    .build());
            
            List<Message> sqsMessages = responseDlq.messages();
            if(sqsMessages.isEmpty()) {
				System.out.println("ℹ️ No messages found.");
				return List.of(); // return empty list instead of null (cleaner for JSON)
			}

            // Extract message bodies
            List<String> messageBodies = sqsMessages.stream()
		            .map(Message::body)
		            .toList();
			
		    // Delete messages after extracting bodies
			for (Message msg : sqsMessages) {
	            System.out.println("📥 Received: " + msg.body());
	            // After processing, delete it
	            deleteMessage(dlqURL, msg);
	            System.out.println("Deleted the messages from the DLQ");
	        }
			return messageBodies;
		} catch(Exception e) {
			throw new RuntimeException("Error while reading from DLQ", e);
		}
		
	}
	
	/**
     * Delete a message from the queue.
     */
	private void deleteMessage(String queueUrl, Message msg) {
		sqsClient.deleteMessage(DeleteMessageRequest.builder()
				.queueUrl(queueUrl)
				.receiptHandle(msg.receiptHandle())
				.build());
		System.out.println("🗑️ Deleted message: " + msg.body());
	}
	
	/**
     * Attach DLQ if not already attached.
     */
	private void attachDLQ(String mainQueueUrl, String dlqArn) {
		GetQueueAttributesResponse attrs = sqsClient.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                        .queueUrl(mainQueueUrl)
                        .attributeNames(QueueAttributeName.REDRIVE_POLICY)
                        .build());

        String redrivePolicy = attrs.attributes().get(QueueAttributeName.REDRIVE_POLICY);

        if (redrivePolicy == null || !redrivePolicy.contains(dlqArn)) {
            sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder()
                            .queueUrl(mainQueueUrl)
                            .attributes(Map.of(
                                    QueueAttributeName.REDRIVE_POLICY,
                                    String.format("{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"5\"}", dlqArn)
                            ))
                            .build());
            System.out.println("🔗 DLQ attached to Main Queue.");
        } else {
            System.out.println("ℹ️ DLQ already attached.");
        }
	}

	/**
     * Helper to get Queue ARN.
     */
	private String getQueueArn(String queueUrl) {
		GetQueueAttributesResponse attr = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
													.queueUrl(queueUrl)
													.attributeNames(QueueAttributeName.QUEUE_ARN)
													.build());
		return attr.attributes().get(QueueAttributeName.QUEUE_ARN);
	}

}
