package com.clip.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(exclude = {
		org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
public class ClipServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClipServerApplication.class, args);
	}

}
