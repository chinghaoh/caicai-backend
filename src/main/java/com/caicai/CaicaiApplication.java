package com.caicai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CaicaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CaicaiApplication.class, args);
	}

}
