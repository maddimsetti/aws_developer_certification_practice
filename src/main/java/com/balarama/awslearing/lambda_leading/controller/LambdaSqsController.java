package com.balarama.awslearing.lambda_leading.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.balarama.awslearing.lambda_leading.DTO.APIResponse;
import com.balarama.awslearing.lambda_leading.service.LambdaSqsMapperService;
import com.balarama.awslearing.lambda_leading.service.SqsService;

@RestController
@RequestMapping("/api/mapping")
public class LambdaSqsController {
	private final LambdaSqsMapperService mapper;
	private final SqsService sqsService;
	private final String LAMBDANAME = "consumer_lambda";

    public LambdaSqsController(LambdaSqsMapperService mapper, SqsService sqsService) {
        this.mapper = mapper;
        this.sqsService = sqsService;
    }
    
    /**
     * Example:
     * POST /api/mapping/sqs-to-lambda?batchSize=5
     */
    @PostMapping("/sqs-to-lambda")
    public ResponseEntity<APIResponse> lambdaMap(
            @RequestParam(defaultValue = "5") int batchSize) {
    	try {
    		String queueArn = sqsService.setupQueues();
    		String msg = mapper.createMapping(queueArn, LAMBDANAME, batchSize);
            return ResponseEntity.ok(new APIResponse("success", msg));
    	} catch(Exception e) {
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse("error", "Failed to create mapping", e.getMessage()));
    	}
    }

}
