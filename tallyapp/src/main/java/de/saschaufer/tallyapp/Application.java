package de.saschaufer.tallyapp;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = {"de.saschaufer.tallyapp.config"})
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
