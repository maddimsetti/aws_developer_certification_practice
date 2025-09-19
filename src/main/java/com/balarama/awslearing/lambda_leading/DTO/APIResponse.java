package com.balarama.awslearing.lambda_leading.DTO;

public class APIResponse {
	private String message;
	private String data;
	private String status;   // "success" or "error"
   
	public APIResponse(String message, String data) {
		this.message = message;
		this.data = data;
	}
	
	public APIResponse(String message, String data, String status) {
		this.message = message;
		this.data = data;
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public String getData() {
		return data;
	}
	
	public String getStatus() {
		return status;
	}
}
