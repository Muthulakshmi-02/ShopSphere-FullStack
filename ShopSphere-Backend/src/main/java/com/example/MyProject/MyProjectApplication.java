package com.example.MyProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MyProjectApplication {


	public static void main(String[] args) {
		SpringApplication.run(MyProjectApplication.class, args);
	}

}
