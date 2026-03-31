package it.icron.icronium.connector.rr;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IcroniumApplication {

	public static final Path WORK_DIR = Paths.get(System.getProperty("user.home"), "icronium-work");

    public static void main(String[] args) {
        SpringApplication.run(IcroniumApplication.class, args);
    }
}
