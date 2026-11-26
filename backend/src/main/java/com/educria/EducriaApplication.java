package com.educria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada do backend. {@code @SpringBootApplication} ativa a configuração do Spring Boot
 * e procura componentes a partir do pacote {@code com.educria}.
 */
@SpringBootApplication
@EnableRetry
@EnableScheduling
public class EducriaApplication {

    /** Registra o relógio de estudo no Spring, permitindo injetar um relógio fixo nos testes. */
    @Bean
    Clock relogioDeEstudo() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(EducriaApplication.class, args);
    }
}
