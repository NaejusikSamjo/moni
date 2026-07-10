package com.moni.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
public class PortfolioServiceApplication {

    private static final String DEFAULT_TIME_ZONE = "Asia/Seoul";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}
