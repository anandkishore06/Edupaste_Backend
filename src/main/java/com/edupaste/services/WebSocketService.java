package com.edupaste.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void publishStatusUpdate(String applicationNumber, Object trackingPayload) {
        if (messagingTemplate == null || applicationNumber == null) return;
        String destination = "/topic/admissions/status/" + applicationNumber.trim();
        try {
            logger.info("Broadcasting WebSocket Status Update to destination: {}", destination);
            messagingTemplate.convertAndSend(destination, trackingPayload);
        } catch (Exception e) {
            logger.error("Failed to broadcast WebSocket status update to {}: {}", destination, e.getMessage());
        }
    }

    public void publishNewApplicationNotification(Long schoolId, Map<String, Object> notificationPayload) {
        if (messagingTemplate == null || schoolId == null) return;
        String destination = "/topic/schools/" + schoolId + "/admissions";
        try {
            logger.info("Broadcasting WebSocket New Application Notification to destination: {}", destination);
            messagingTemplate.convertAndSend(destination, notificationPayload);
        } catch (Exception e) {
            logger.error("Failed to broadcast WebSocket notification to {}: {}", destination, e.getMessage());
        }
    }
}
