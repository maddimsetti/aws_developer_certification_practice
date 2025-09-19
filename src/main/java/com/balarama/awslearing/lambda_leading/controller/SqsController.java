package com.balarama.awslearing.lambda_leading.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.balarama.awslearing.lambda_leading.DTO.APIResponse;
import com.balarama.awslearing.lambda_leading.service.SqsService;

@RestController
@RequestMapping("/api/files")
public class SqsController {

	private final SqsService sqsService;
	
	public SqsController(SqsService sqsService) {
		this.sqsService = sqsService;
	}
	
	/**
     * Initialize queues (Main + DLQ).
     */
	@PostMapping("/setup")
	public ResponseEntity<APIResponse> setupQueues(RequestEntity<Void> request) {
		try {
			sqsService.setupQueues();
			return ResponseEntity.status(HttpStatus.OK)
					.body(new APIResponse("success", "✅ SQS and DLQ setup complete"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new APIResponse("Error in setup SQS or DLQ", e.getMessage()));
		}
	}

    /**
     * Receive messages from SQS.
     */
	@GetMapping("/receivemessages")
	public ResponseEntity<APIResponse> receiveMessages(RequestEntity<Void> request) {
        try {
            sqsService.receiveMessage();
            return ResponseEntity.status(HttpStatus.OK).body(new APIResponse("success", "📥 Checked for messages"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse("error", e.getMessage()));
        }
    }
	
	@GetMapping("/dlq/receive")
    public ResponseEntity<APIResponse> receiveDlqMessages(RequestEntity<Void> request) {
        try {
            sqsService.receiveDlqMessages();
            return ResponseEntity.status(HttpStatus.OK).body(new APIResponse("success", "📥 Checked for DLQ messages"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse("error", e.getMessage()));
        }
    }
}
