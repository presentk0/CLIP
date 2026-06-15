package com.clip.server.admin.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    private final String username;
    private final String password;

    public AdminProperties(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
