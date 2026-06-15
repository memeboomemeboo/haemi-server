package com.memeboo2.haemi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HaemiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HaemiApplication.class, args);
	}

}
