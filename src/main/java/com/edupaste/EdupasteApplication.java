package com.edupaste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.edupaste.repositories")
@EntityScan(basePackages = "com.edupaste.models")
public class EdupasteApplication {
    public static void main(String[] args) {
        SpringApplication.run(EdupasteApplication.class, args);
    }
}
