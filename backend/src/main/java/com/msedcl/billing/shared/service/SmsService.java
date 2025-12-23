package com.msedcl.billing.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${notifications.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notifications.sms.gateway-url:}")
    private String gatewayUrl;

    @Value("${notifications.sms.api-key:}")
    private String apiKey;

    @Value("${notifications.sms.sender:}")
    private String senderId;

    public void sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.debug("SMS notifications disabled; skipping send to {}", phoneNumber);
            return;
        }

        if (!StringUtils.hasText(gatewayUrl)) {
            log.warn("SMS gateway URL not configured; unable to send message to {}", phoneNumber);
            return;
        }

        if (!StringUtils.hasText(phoneNumber)) {
            log.debug("No phone number available for SMS notification");
            return;
        }

        try {
            RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

            Map<String, Object> payload = Map.of(
                "to", phoneNumber,
                "message", message,
                "sender", senderId,
                "apiKey", apiKey
            );

            RequestEntity<Map<String, Object>> request = RequestEntity
                .post(URI.create(gatewayUrl))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);

            restTemplate.exchange(request, Void.class);
            log.debug("Sent SMS notification to {}", phoneNumber);
        } catch (Exception ex) {
            log.error("Failed to send SMS to {}", phoneNumber, ex);
        }
    }
}
