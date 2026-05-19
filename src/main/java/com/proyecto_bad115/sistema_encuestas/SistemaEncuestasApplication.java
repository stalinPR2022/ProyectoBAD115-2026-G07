package com.proyecto_bad115.sistema_encuestas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.proyecto_bad115.sistema_encuestas.model")
public class SistemaEncuestasApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaEncuestasApplication.class, args);
	}

}
