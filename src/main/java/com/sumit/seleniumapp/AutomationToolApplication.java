package com.sumit.seleniumapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AutomationToolApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomationToolApplication.class, args);
	}

}
