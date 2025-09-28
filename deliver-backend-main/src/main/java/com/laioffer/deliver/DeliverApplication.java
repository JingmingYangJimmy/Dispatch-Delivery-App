package com.laioffer.deliver;

import com.laioffer.deliver.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(JwtProperties.class)
@SpringBootApplication
public class DeliverApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliverApplication.class, args);
    }

}
