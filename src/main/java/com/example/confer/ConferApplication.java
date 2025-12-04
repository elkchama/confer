package com.example.confer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ConferApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConferApplication.class, args);
	}

}
