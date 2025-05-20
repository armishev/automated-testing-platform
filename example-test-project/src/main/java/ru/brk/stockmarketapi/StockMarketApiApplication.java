package ru.brk.stockmarketapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockMarketApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockMarketApiApplication.class, args);
    }

}
