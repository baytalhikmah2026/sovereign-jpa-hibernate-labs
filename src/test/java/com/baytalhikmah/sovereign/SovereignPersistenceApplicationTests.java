package com.baytalhikmah.sovereign;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SovereignPersistenceApplicationTests {

	@Test
	void contextLoads() {
	}

}
