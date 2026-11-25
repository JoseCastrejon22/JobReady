package com.jobready.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Los correos de recuperación se envían en segundo plano.
public class JobReadyApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobReadyApplication.class, args);
    }

}

