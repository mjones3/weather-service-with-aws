package io.melvinjones.weatherservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeatherApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WeatherApplication.class);

    @Autowired
    WeatherServiceManager weatherServiceManager;


    public static void main(String[] args) {
        SpringApplication.run(WeatherApplication.class, args);
    }

    public void run(String... args) {

        weatherServiceManager.run();
    }
}

