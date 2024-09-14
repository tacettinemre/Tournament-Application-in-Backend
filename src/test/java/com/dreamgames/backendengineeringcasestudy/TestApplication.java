package com.dreamgames.backendengineeringcasestudy;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class TestApplication {
    // This is a placeholder class to customize Spring Boot configuration for tests
}
