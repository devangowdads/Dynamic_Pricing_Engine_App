package com.pricing.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DynamicPricingEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(DynamicPricingEngineApplication.class, args);
	}

}
