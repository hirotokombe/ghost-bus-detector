package com.hirokombe.ghostbus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GhostBusDetectorApplicationTests {

	@Test
	void contextLoads() {
	}

}
