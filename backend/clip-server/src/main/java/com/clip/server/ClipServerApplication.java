package com.clip.server;

import com.clip.server.admin.config.AdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(exclude = {
		org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@EnableRetry
@EnableConfigurationProperties(AdminProperties.class)
public class ClipServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClipServerApplication.class, args);
	}

}
