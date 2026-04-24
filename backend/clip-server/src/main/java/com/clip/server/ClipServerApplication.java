package com.clip.server;

import com.clip.server.quiz.service.GeminiService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClipServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClipServerApplication.class, args);
	}

}
