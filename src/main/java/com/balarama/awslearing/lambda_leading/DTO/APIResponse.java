package com.balarama.awslearing.lambda_leading.DTO;

public class APIResponse {
	private String message;
	private String data;

	public APIResponse(String message, String data) {
		this.message = message;
		this.data = data;
	}

	public String getMessage() {
		return message;
	}

	public String getData() {
		return data;
	}
}
