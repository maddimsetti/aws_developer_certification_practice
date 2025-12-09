package com.balarama.awslearing.lambda_leading.eventhandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class AuthorizerHandlerRequest implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		String token = null;
        try {
            Map<String, String> headers = (Map<String, String>) input.get("headers");
            token = headers.get("Authorization");
            context.getLogger().log("📩 token value: " + token);
        } catch (Exception e) {
            // No header, fail early
        	context.getLogger().log("📩 failure event: " + e.getMessage());
            return generatePolicy("unknown", "Deny", getMethodArn(input));
        }
        
        if ("Bearer mysecrettoken".equals(token)) {
            return generatePolicy("user123", "Allow", getMethodArn(input));
        } else {
            return generatePolicy("unauthorized", "Deny", getMethodArn(input));
        }
	}
	
	private String getMethodArn(Map<String, Object> input) {
		
        return (String) input.get("methodArn");
    }

	private Map<String, Object> generatePolicy(String principalId, String effect, String resource) {
        Map<String, Object> policy = new HashMap<>();
        policy.put("principalId", principalId);

        Map<String, Object> statement = new HashMap<>();
        statement.put("Action", "execute-api:Invoke");
        statement.put("Effect", effect);
        statement.put("Resource", resource);

        Map<String, Object> policyDocument = new HashMap<>();
        policyDocument.put("Version", "2012-10-17");
        policyDocument.put("Statement", Collections.singletonList(statement));

        policy.put("policyDocument", policyDocument);
        
        // ✅ Add context here
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("userId", principalId);
        contextMap.put("role", "admin");
        contextMap.put("env", "dev");

        policy.put("context", contextMap);  // This gets passed to the backend Lambda
        System.out.println("Generated the policy" + policy);

        return policy;
    }
}
