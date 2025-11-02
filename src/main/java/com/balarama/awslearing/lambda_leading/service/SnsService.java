package com.balarama.awslearing.lambda_leading.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

@Service
public class SnsService {

    private final SnsClient snsclient;
    private static final String SNS_TOPIC_NAME = "file-upload-notifications";
    private String topicArn;

    public SnsService(SnsClient snsclient) {
        this.snsclient = snsclient;
    }

    // ✅ Create topic (idempotent)
    public String createTopic() {
        CreateTopicResponse response = snsclient.createTopic(
                CreateTopicRequest.builder().name(SNS_TOPIC_NAME).build());
        this.topicArn = response.topicArn();
        return topicArn;
    }

    // ✅ Generic subscription (email, sqs, sms, http, etc.)
    public void subscribe(String protocol, String endpoint) {
        if (topicArn == null) {
            topicArn = createTopic();
        }

        ListSubscriptionsByTopicResponse subs = snsclient.listSubscriptionsByTopic(
                ListSubscriptionsByTopicRequest.builder()
                        .topicArn(topicArn)
                        .build());

        boolean alreadySubscribed = subs.subscriptions().stream()
                .anyMatch(s -> s.endpoint().equalsIgnoreCase(endpoint) && s.protocol().equals(protocol));

        if (!alreadySubscribed) {
            SubscribeResponse response = snsclient.subscribe(SubscribeRequest.builder()
                    .protocol(protocol)
                    .endpoint(endpoint)
                    .topicArn(topicArn)
                    .build());

            System.out.println("✅ Subscription created for " + endpoint +
                               " (protocol=" + protocol + ") ARN=" + response.subscriptionArn());
        } else {
            System.out.println("⚠️ Already subscribed: " + endpoint + " (protocol=" + protocol + ")");
        }
    }

    // ✅ Convenience wrappers
    public void subscribeEmail(String email) {
        subscribe("email", email);
    }

    public void subscribeSqs(String sqsQueueArn) {
        subscribe("sqs", sqsQueueArn);   // Subscribe an SQS queue (ARN, not URL)
    }

    // ✅ Publish a message
    public PublishResponse publish(String bucketname, String fileName, long filesize) {
        if (topicArn == null) {
            topicArn = createTopic();
        }

        String message = String.format("📂 File uploaded: %s (%d bytes) in bucket: %s",
                fileName, filesize, bucketname);

        PublishResponse response = snsclient.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .subject("New File Upload Notification")
                .message(message)
                .build());

        System.out.println("✅ Published SNS Notification : " + message);
        return response;
    }

    // Getter
    public String getTopicArn() {
        return topicArn;
    }
}
