package com.balarama.awslearing.lambda_leading.service;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.ComparisonOperator;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

@Service
public class DlqMonitoringService {
	private final CloudWatchClient cloudWatch;
	private final SnsService snsService;
	
	public DlqMonitoringService(CloudWatchClient cloudWatch, SnsService snsService) {
		this.cloudWatch = cloudWatch;
		this.snsService = snsService;
	}
	
	public String createAlarmForDlq(String queueName, String dlqArnOrQueueName, String email) {
		// 1) Create SNS topic for alerting
		String topicArn = snsService.createTopic();
		
		//checking for subscription
		snsService.subscribeEmail(email);
		
		// 2) Create CloudWatch alarm
        String alarmName = "DLQ-" + queueName + "-HasMessages";   //queue name is Main Queue
        // Metric: ApproximateNumberOfMessagesVisible in name space AWS/SQS
        PutMetricAlarmRequest alarmReq = PutMetricAlarmRequest.builder()
        									.alarmName(alarmName)
        									.comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
        									.evaluationPeriods(1)
        									.threshold(0.0)
        									.metricName("ApproximateNumberOfMessagesVisible")
        									.namespace("AWS/SQS")
        									.period(60)
        									.statistic(Statistic.SAMPLE_COUNT)
        									.alarmActions(topicArn)
        									.dimensions(Dimension.builder().name("QueueName").value(alarmName).build())
        									.build();
        
        cloudWatch.putMetricAlarm(alarmReq);
		
        return "Alarm created: " + alarmName + ", SNS topic: " + topicArn;
	}
}
