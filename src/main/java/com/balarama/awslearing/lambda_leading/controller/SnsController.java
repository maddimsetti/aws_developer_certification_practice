package com.balarama.awslearing.lambda_leading.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.balarama.awslearing.lambda_leading.DTO.APIResponse;
import com.balarama.awslearing.lambda_leading.service.SnsService;
import com.balarama.awslearing.lambda_leading.service.SqsService;

@RestController
@RequestMapping("/api/files")
public class SnsController {
	
	private final SnsService snsService;
	private final SqsService sqsService;
	
	public SnsController(SnsService snsService, SqsService sqsService) {
		this.snsService = snsService;
		this.sqsService = sqsService;
	}
	
	// Create topic
	@PostMapping("/create")
	public ResponseEntity<APIResponse> createTopic() {
		try {
			String topicArn = snsService.createTopic();
			return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse("SNS Topic created successfully", topicArn));
		} catch(Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse("Failed to create SNS Topic", e.getMessage()));
		}
	}
	
	// Subscribe email
	@PostMapping("/subscribe/email")
	public ResponseEntity<APIResponse> subscribeEmail(@RequestParam String email) {
		try {
			snsService.subscribeEmail(email);
			return ResponseEntity
					.ok(new APIResponse("Subscription requested", "Check your inbox to confirm: " + email));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new APIResponse("Failed to subscribe email", e.getMessage()));
		}
	}

	// Subscribe an SQS queue (ARN, not URL)
	@PostMapping("/subscribe/sqs")
	public ResponseEntity<APIResponse> subscribeSqs() {
		try {
			String sqsMainArn = sqsService.setupQueues();
			snsService.subscribeSqs(sqsMainArn);
			return ResponseEntity
					.ok(new APIResponse("Subscription successful", " with sqsArn : " + sqsMainArn));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new APIResponse("Failed to subscribe sqs", e.getMessage()));
		}
	}

}
