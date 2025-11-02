package com.balarama.awslearing.lambda_leading.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.balarama.awslearing.lambda_leading.DTO.APIResponse;
import com.balarama.awslearing.lambda_leading.service.FileValidatorServiceAlarm;

@RestController
@RequestMapping("/api/mointor")
public class S3UploadMointoringController {
	
	private final FileValidatorServiceAlarm fileValidatorServiceAlarm;
	
	public S3UploadMointoringController(FileValidatorServiceAlarm fileValidatorServiceAlarm) {
		this.fileValidatorServiceAlarm = fileValidatorServiceAlarm;
	}
	
	/**
     * Example:
     * POST /api/monitor/s3-upload-create-alarm?&email=test@example.com
     */
	@PostMapping("/s3-upload-create-alarm")
	public ResponseEntity<APIResponse> createS3UploadAlarm(
			@RequestParam String email) {
		try {
			String message = fileValidatorServiceAlarm.createInvalidFileAlarm(email);
			return ResponseEntity.ok(new APIResponse("success", message)); 
		} catch(Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse("error", "Failed to create mointoring alarm", e.getMessage()));
		}
	}

}
