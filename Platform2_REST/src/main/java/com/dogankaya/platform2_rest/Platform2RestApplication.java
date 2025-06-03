package com.dogankaya.platform2_rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Platform2RestApplication {

    public static void main(String[] args) {
        SpringApplication.run(Platform2RestApplication.class, args);
    }

}
