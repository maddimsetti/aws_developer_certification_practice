package com.balarama.awslearing.lambda_leading.DTO;

public class APIResponse {
	private String message;
	private Object data;
	private String status;   // "success" or "error"
   
	public APIResponse(String message, String status) {
		this.message = message;
		this.status = status;
	}
	
	public APIResponse(String message, Object data, String status) {
		this.message = message;
		this.data = data;
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public Object getData() {
		return data;
	}
	
	public String getStatus() {
		return status;
	}
}
