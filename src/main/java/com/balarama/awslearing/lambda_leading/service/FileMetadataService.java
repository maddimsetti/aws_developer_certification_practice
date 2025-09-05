package com.balarama.awslearing.lambda_leading.service;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FileMetadataService {

    private static final String TABLE_NAME = "FileMetadata";
    private final DynamoDbClient dynamoDbClient;

    public FileMetadataService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }
    
    @PostConstruct
    public void init() {
        // Run table creation logic after bean is constructed, not inside constructor
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build());
            System.out.println("✅ DynamoDB table already exists");
        } catch (ResourceNotFoundException e) {
            System.out.println("⚡ Creating DynamoDB table: " + TABLE_NAME);
            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        }
    }

    public void saveFileMetadata(String bucket, String filename, long size) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("bucket", AttributeValue.builder().s(bucket).build());
        item.put("filename", AttributeValue.builder().s(filename).build());
        item.put("size", AttributeValue.builder().n(String.valueOf(size)).build());
        item.put("timestamp", AttributeValue.builder().s(Instant.now().toString()).build());

        dynamoDbClient.putItem(builder -> builder.tableName(TABLE_NAME).item(item));

        System.out.println("✅ Metadata saved to DynamoDB for file: " + filename);
    }
}
