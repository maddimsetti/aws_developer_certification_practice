package com.balarama.awslearing.lambda_leading.service;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

@Service
public class SnsService {
	
	private final SnsClient snsclient;
	
	private static final String SNS_TOPIC_NAME = "file-upload-notifications";
	
	private String topicArn;
	
	public SnsService(SnsClient snsclient) {
		this.snsclient = snsclient;
	}
	
	//create topic name (idempotent)
	public String createTopic() {
		CreateTopicResponse response = snsclient.createTopic(CreateTopicRequest.builder().name(SNS_TOPIC_NAME).build());
		
		this.topicArn = response.topicArn();
		return topicArn;
	}
	
	//subscribe email
	public void subscribeEmail(String email) {
	    // List existing subscriptions for this topic
	    ListSubscriptionsByTopicResponse subs = snsclient.listSubscriptionsByTopic(
	        ListSubscriptionsByTopicRequest.builder()
	            .topicArn(topicArn)
	            .build()
	    );

	    boolean alreadySubscribed = subs.subscriptions().stream()
	        .anyMatch(s -> s.endpoint().equalsIgnoreCase(email) && s.protocol().equals("email"));

	    if (!alreadySubscribed) {
	        snsclient.subscribe(SubscribeRequest.builder()
	            .protocol("email")
	            .endpoint(email)
	            .topicArn(topicArn)
	            .build());
	        System.out.println("✅ Subscription request sent to " + email);
	    } else {
	        System.out.println("⚠️ Email already subscribed: " + email);
	    }
	}
	
	public void publish(String bucketname, String fileName, long filesize) {
		String message = "📂 File uploaded in SNS	: " + fileName + " (" + filesize + " bytes) in bucket: " + bucketname;
		String cachedTopicArn = null;
		if (topicArn != null) {
            cachedTopicArn = topicArn;
        } else {
        	cachedTopicArn = createTopic();
        }
		snsclient.publish(PublishRequest.builder()
                .topicArn(cachedTopicArn)
                .subject("New File Upload Notification")
                .message(message)
                .build());
		System.out.println("✅ Published SNS Notification: " + message);
	}
	
	// Get TopicArn for Lambda usage
    public String getTopicArn() {
        return topicArn;
    }
}
