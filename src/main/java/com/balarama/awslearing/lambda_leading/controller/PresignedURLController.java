package com.balarama.awslearing.lambda_leading.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.balarama.awslearing.lambda_leading.service.PresignedURLService;

@RestController
@RequestMapping("/api/files")
public class PresignedURLController {
	
	private final PresignedURLService presignedURLService;
	
	public PresignedURLController(PresignedURLService presignedURLService) {
		this.presignedURLService = presignedURLService;
	}
	
	@GetMapping("/presign")
	public ResponseEntity<Map<String, String>> getPresigndURL(@RequestParam String fileName,
            @RequestParam(defaultValue = "application/pdf") String contentType,
            @RequestParam(defaultValue = "5") int expiryTime,
            @RequestParam(defaultValue = "5242880") long maxSizeBytes) {                 //size = 5 MB
		String url = presignedURLService.generateUploadURL(fileName, contentType, expiryTime, maxSizeBytes);
		return ResponseEntity.ok(Map.of("url", url));
	}
	
	// ✅ Presigned GET (download)
    @GetMapping("/presignDownload")
    public ResponseEntity<Map<String, String>> generateDownloadUrl(
            @RequestParam String fileName,
            @RequestParam(defaultValue = "5") int expirationTime) {
    	String url = presignedURLService.generateDownloadURL(fileName, expirationTime);
    	return ResponseEntity.ok(Map.of("url", url));
    }
    
    @GetMapping("/presignPost")
    public ResponseEntity<Map<String, String>> generatePresignedPost(
            @RequestParam String fileName,
            @RequestParam(defaultValue = "application/pdf") String contentType,
            @RequestParam(defaultValue = "5") int expiryTime,
            @RequestParam(defaultValue = "5242880") long maxSizeBytes // default 5 MB
    ) throws Exception {
    	String response = presignedURLService.generatePostUploadURL(fileName, contentType, expiryTime, maxSizeBytes);
    	return ResponseEntity.ok(Map.of("string", response));
    }

}
