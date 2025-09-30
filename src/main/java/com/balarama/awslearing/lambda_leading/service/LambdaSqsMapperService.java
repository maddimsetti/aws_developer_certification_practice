package com.balarama.awslearing.lambda_leading.service;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingResponse;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsRequest;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsResponse;

@Service
public class LambdaSqsMapperService {
	private final LambdaClient lambdaClient;
	
	public LambdaSqsMapperService(LambdaClient lambdaClient) {
		this.lambdaClient = lambdaClient;
	}
	
	/**
     * Create an event source mapping (idempotent-ish).
     * - queueArn: ARN of SQS queue (not URL)
     * - functionNameOrArn: Lambda function name or ARN
     */
	public String createMapping(String queueArn, String functionNameOrArn, int batchSize) {
		// Check if an existing mapping exists for this combination
		ListEventSourceMappingsRequest listReq = ListEventSourceMappingsRequest.builder()
															.eventSourceArn(queueArn)
															.functionName(functionNameOrArn)
															.build();
		
		ListEventSourceMappingsResponse lisResponse = lambdaClient.listEventSourceMappings(listReq);
		if(!lisResponse.eventSourceMappings().isEmpty()) {
			return "Mapping already exists: " + lisResponse.eventSourceMappings().get(0).uuid();
		}
		
		CreateEventSourceMappingRequest req = CreateEventSourceMappingRequest.builder()
                .eventSourceArn(queueArn)
                .functionName(functionNameOrArn)
                .batchSize(batchSize)
                .enabled(true)
                .build();
		
		CreateEventSourceMappingResponse resp = lambdaClient.createEventSourceMapping(req);
        return "Created mapping id: " + resp.uuid();
	}
}
