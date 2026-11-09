package com.baytalhikmah.sovereign;

import org.springframework.boot.SpringApplication;

public class TestSovereignPersistenceApplication {

	public static void main(String[] args) {
		SpringApplication.from(SovereignPersistenceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
