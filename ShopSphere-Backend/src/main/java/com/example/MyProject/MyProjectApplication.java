package com.example.MyProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
// Both were missing:
//  @EnableScheduling - without it the 30-minute job that cancels abandoned card orders (and puts
//                      their stock back) NEVER runs, so unpaid orders hold stock forever.
//  @EnableAsync      - without it @Async is ignored, so order emails are sent on the request thread
//                      (slow checkout) instead of in the background.
@EnableScheduling
@EnableAsync
public class MyProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyProjectApplication.class, args);
    }
}