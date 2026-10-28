package com.A509.Security;

import com.A509.Untils.JwtUntilHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URI;
import java.net.http.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(ErrorDispatchIntegrationTest.ErrorProbeConfig.class)
class ErrorDispatchIntegrationTest {
    @LocalServerPort int port;
    @MockitoBean(name = "syncUniformCategoryNames") CommandLineRunner initializer;
    @MockitoBean CustomUserDetailService users;
    @Autowired JwtUntilHelper jwt;
    final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach void setUp() {
        when(users.loadUserByUsername("regression-admin"))
                .thenReturn(User.withUsername("regression-admin").password("test-only").authorities("ADMIN").build());
    }

    @Test void missingAdminApiMustNotBecomeAnAuthenticationFailure() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/users/admin/not-implemented"))
                .header("Authorization", "Bearer " + jwt.generateToken("regression-admin")).GET().build();
        assertEquals(404, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    @Test void missingPublicArticleMustNotBecomeAnAuthenticationFailure() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/articles/does-not-exist"))
                .header("Authorization", "Bearer " + jwt.generateToken("regression-admin")).GET().build();
        assertEquals(404, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    @Test void serverErrorsMustNotBecomeAuthenticationFailures() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/articles/regression-server-error"))
                .header("Authorization", "Bearer " + jwt.generateToken("regression-admin")).GET().build();
        assertEquals(500, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    @TestConfiguration static class ErrorProbeConfig {
        @Bean ErrorProbe errorProbe() { return new ErrorProbe(); }
    }

    @RestController @TestComponent static class ErrorProbe {
        @GetMapping("/api/articles/regression-server-error") String fail() {
            throw new IllegalStateException("Intentional test-only server failure");
        }
    }
}
