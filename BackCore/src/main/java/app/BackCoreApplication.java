package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
    scanBasePackages = { 
        "app", // Escanea el paquete base y sus subpaquetes (utils, security, config)
        "controllers", 
        "services", 
        "repositories", 
        "entities" 
    }
)
@EnableJpaRepositories(basePackages = "repositories")
@EntityScan(basePackages = "entities")
public class BackCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackCoreApplication.class, args);
    }
}