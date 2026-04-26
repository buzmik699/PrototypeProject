package buzmakov.Prototype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PrototypeApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrototypeApplication.class, args);
    }
}