package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = { 
        "app",
        "controllers",
        "services", 
        "repositories", 
        "entities" 
    }
)
@EnableJpaRepositories(basePackages = "repositories")
@EntityScan(basePackages = "entities")
@EnableScheduling
public class BackCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackCoreApplication.class, args);
    }
}