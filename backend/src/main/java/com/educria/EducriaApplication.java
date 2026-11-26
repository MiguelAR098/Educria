package com.educria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableScheduling
public class EducriaApplication {

    @Bean
    Clock relogioDeEstudo() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(EducriaApplication.class, args);
    }
}
