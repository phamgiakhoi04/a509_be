package com.A509;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.CommandLineRunner;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class A509ApplicationTests {

    // The startup initializer must never change data during a test.
    @MockitoBean(name = "syncUniformCategoryNames")
    CommandLineRunner initializer;

	@Test
	void contextLoads() {
	}

}
