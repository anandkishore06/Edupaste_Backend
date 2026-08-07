package com.edupaste.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateOrRephraseRemarks(String text, String status) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            logger.warn("Gemini API key is not configured.");
            return text != null ? text : "";
        }

        String promptContext = "You are an AI assistant helping a school administrator write a polite and professional message body to a parent regarding their child's admission application. " +
                "The current status of the application is: " + status + ". " +
                "IMPORTANT RULES: " +
                "1. Do not include any greetings (e.g., 'Dear Parent'). " +
                "2. Do not include any sign-offs (e.g., 'Sincerely', 'The School'). " +
                "3. Do not specify any names. " +
                "4. Only generate the raw body text of the message. " +
                "5. Include a polite 'Best of luck!' or similar encouraging message at the end. ";

        if (text == null || text.trim().isEmpty()) {
            promptContext += "Please generate a short, standard, polite message body regarding this status update.";
        } else {
            promptContext += "Please rephrase the following notes into a professional, polite message body. " +
                    "If the notes are very brief, expand them into full, courteous sentences. " +
                    "Notes to rephrase: " + text;
        }

        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + geminiApiKey;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> parts = new HashMap<>();
            parts.put("text", promptContext);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(parts));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(content));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    Map<String, Object> contentMap = (Map<String, Object>) firstCandidate.get("content");
                    if (contentMap != null && contentMap.containsKey("parts")) {
                        List<Map<String, Object>> resParts = (List<Map<String, Object>>) contentMap.get("parts");
                        if (!resParts.isEmpty()) {
                            return (String) resParts.get(0).get("text");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to generate AI remarks", e);
        }

        return text != null ? text : "";
    }
}
