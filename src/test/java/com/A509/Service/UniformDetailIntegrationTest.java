package com.A509.Service;

import com.A509.Entity.Country;
import com.A509.Entity.Image;
import com.A509.Entity.Uniform;
import com.A509.Entity.UniformCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties", properties = {
        "spring.datasource.url=jdbc:h2:mem:a509-uniform-detail;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.open-in-view=false",
        "server.address=127.0.0.1"
})
class UniformDetailIntegrationTest {
    private static final String PUBLIC_IMAGE = "https://example.test/public.jpg";
    private static final String SENSITIVE_IMAGE = "https://example.test/sensitive.jpg";

    @LocalServerPort int port;
    @MockitoBean(name = "syncUniformCategoryNames") CommandLineRunner initializer;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper mapper;
    @Autowired TranslationService translations;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test void detailsLoadWithCategoriesAndImagesAfterTheDatabaseSessionCloses() throws Exception {
        Fixture fixture = new TransactionTemplate(transactions).execute(status -> createFixture());

        assertAll(
                () -> assertDetails(fixture, "vi", "Áo phục dựng", "Đơn vị mẫu", "Giai đoạn mẫu"),
                () -> assertDetails(fixture, "en", "Reenactment shirt", "Example unit", "Example period"),
                () -> {
                    JsonNode legacy = getUniform(fixture.legacyId(), "vi");
                    assertEquals("Quân trang cũ", legacy.path("name").asString());
                    assertTrue(legacy.path("category").isNull());
                    assertTrue(legacy.path("periodCategory").isNull());
                    assertTrue(legacy.path("images").isArray());
                    assertTrue(legacy.path("images").isEmpty());
                }
        );
    }

    private Fixture createFixture() {
        Country country = Country.builder().countryName("Việt Nam").build();
        em.persist(country);
        UniformCategory force = UniformCategory.builder().categoryName("Đơn vị mẫu")
                .countryId(country.getId()).categoryType(UniformCategory.CategoryType.REENACTMENT).build();
        em.persist(force);
        UniformCategory period = UniformCategory.builder().categoryName("Giai đoạn mẫu").slug("giai-doan-mau")
                .countryId(country.getId()).parent(force).categoryType(UniformCategory.CategoryType.REENACTMENT).build();
        em.persist(period);
        Uniform uniform = Uniform.builder().name("Áo phục dựng").country(country)
                .category(force).periodCategory(period).period(period.getSlug()).build();
        em.persist(uniform);
        em.persist(Image.builder().uniform(uniform).imageUrl(PUBLIC_IMAGE).build());
        em.persist(Image.builder().uniform(uniform).imageUrl(SENSITIVE_IMAGE).sensitive(true).build());
        Uniform legacy = Uniform.builder().name("Quân trang cũ").country(country).build();
        em.persist(legacy);
        em.flush();

        translations.save("UNIFORM", uniform.getId(), new TranslationService.Update(
                Map.of("name", "Reenactment shirt"), Map.of("name", uniform.getName())));
        translations.save("UNIFORM_CATEGORY", force.getId(), new TranslationService.Update(
                Map.of("categoryName", "Example unit"), Map.of("categoryName", force.getCategoryName())));
        translations.save("UNIFORM_CATEGORY", period.getId(), new TranslationService.Update(
                Map.of("categoryName", "Example period"), Map.of("categoryName", period.getCategoryName())));
        return new Fixture(uniform.getId(), force.getId(), period.getId(), legacy.getId());
    }

    private void assertDetails(Fixture fixture, String language, String name, String category, String period)
            throws Exception {
        JsonNode body = getUniform(fixture.uniformId(), language);
        assertEquals(name, body.path("name").asString());
        assertEquals("Việt Nam", body.path("country").path("countryName").asString());
        assertEquals(fixture.categoryId(), body.path("category").path("id").asLong());
        assertEquals(category, body.path("category").path("categoryName").asString());
        assertEquals(fixture.periodId(), body.path("periodCategory").path("id").asLong());
        assertEquals(period, body.path("periodCategory").path("categoryName").asString());

        JsonNode images = body.path("images");
        assertTrue(images.isArray());
        assertEquals(2, images.size());
        int sensitiveImages = 0;
        for (JsonNode image : images) {
            if (image.path("sensitive").asBoolean()) {
                sensitiveImages++;
                assertTrue(image.path("imageUrl").isNull());
            } else {
                assertEquals(PUBLIC_IMAGE, image.path("imageUrl").asString());
            }
        }
        assertEquals(1, sensitiveImages);
        assertFalse(body.toString().contains(SENSITIVE_IMAGE));
    }

    private JsonNode getUniform(long id, String language) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/uniforms/" + id))
                .header("X-A509-Language", language).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), () -> language + ": " + response.body());
        return mapper.readTree(response.body());
    }

    private record Fixture(long uniformId, long categoryId, long periodId, long legacyId) {}
}
