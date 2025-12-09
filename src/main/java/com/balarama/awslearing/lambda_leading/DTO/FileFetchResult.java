package com.balarama.awslearing.lambda_leading.DTO;

public class FileFetchResult {
	public boolean exists;
    public byte[] fileContent;   // null if file not found
    public String message;

    public FileFetchResult(boolean exists, byte[] fileContent, String message) {
        this.exists = exists;
        this.fileContent = fileContent;
        this.message = message;
    }
}
