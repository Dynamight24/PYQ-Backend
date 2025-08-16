package com.uietpapers.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminConfig {
    @Value("${app.admin-key}")
    private String adminKey;

    public String getAdminKey() {
        return adminKey;
    }
}
