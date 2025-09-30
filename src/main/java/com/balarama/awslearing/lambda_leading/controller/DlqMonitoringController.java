package com.balarama.awslearing.lambda_leading.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.balarama.awslearing.lambda_leading.service.DlqMonitoringService;
import com.balarama.awslearing.lambda_leading.service.SqsService;

@RestController
@RequestMapping("/api/monitor")
public class DlqMonitoringController {

	private static final String MAINQUEUENAME = "file-upload-queue";
	private static final String DLQNAME = "file-upload-dlq";
	
    private final DlqMonitoringService dlqMonitoringService;

    public DlqMonitoringController(DlqMonitoringService dlqMonitoringService) {
        this.dlqMonitoringService = dlqMonitoringService;
    }

    /**
     * Example:
     * POST /api/monitor/dlq-create-alarm?&email=test@example.com
     */
    @PostMapping("/dlq-create-alarm")
    public String createDlqAlarm(
            @RequestParam String email) {

        return dlqMonitoringService.createAlarmForDlq(MAINQUEUENAME, DLQNAME, email);
    }
}
