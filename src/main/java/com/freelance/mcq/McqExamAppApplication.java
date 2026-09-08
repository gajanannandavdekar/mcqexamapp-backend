package com.freelance.mcq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*")
@SpringBootApplication
public class McqExamAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(McqExamAppApplication.class, args);
	}

}
