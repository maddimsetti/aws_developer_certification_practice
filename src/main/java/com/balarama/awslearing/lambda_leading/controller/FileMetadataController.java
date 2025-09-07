package com.balarama.awslearing.lambda_leading.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.balarama.awslearing.lambda_leading.service.FileMetadataService;

@RestController
@RequestMapping("api/files")
public class FileMetadataController {
	
	private final FileMetadataService fileMetadataService;
	
	public FileMetadataController(FileMetadataService fileMetadataService) {
		this.fileMetadataService = fileMetadataService;
	}
	
	@GetMapping("/getFiles")
	public List<Map<String, String>> getFiles(@RequestParam(required = false) String name) {
		if (name != null && !name.isEmpty()) {
            return fileMetadataService.getFilesByName(name);
		}
		return fileMetadataService.getAllFiles();
	}
		

}
