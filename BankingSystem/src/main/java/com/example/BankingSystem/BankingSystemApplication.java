package com.example.BankingSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class BankingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankingSystemApplication.class, args);
	}

}
