package com.A509.Service;

import com.A509.Entity.*;
import com.A509.Untils.JwtUntilHelper;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.A509.Security.MediaCacheFilter;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MediaApiTest {
    @MockitoBean(name = "syncUniformCategoryNames") CommandLineRunner initializer;
    @MockitoBean Cloudinary cloud;
    @Autowired EntityManager em;
    @Autowired WebApplicationContext context;
    @Autowired JwtUntilHelper jwt;
    @Autowired MediaCacheFilter cache;
    MockMvc mvc;
    Country country;
    UniformCategory force, period;
    Uniform uniform;
    Article article;
    ArticleImage hidden;
    User admin;

    @BeforeEach void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).addFilters(cache).build();
        for (String name : List.of("USER", "ADMIN")) {
            Role role = Role.builder().name(name).build(); em.persist(role);
            User user = User.builder().username(name.toLowerCase()).email(name + "@example.test")
                    .password("unused").role(role).build(); em.persist(user);
            if (name.equals("ADMIN")) admin = user;
        }
        country = Country.builder().countryName("Test Country").build(); em.persist(country);
        force = UniformCategory.builder().categoryName("Force").countryId(country.getId())
                .categoryType(UniformCategory.CategoryType.REENACTMENT).build(); em.persist(force);
        period = UniformCategory.builder().categoryName("Period").slug("period").countryId(country.getId()).parent(force)
                .categoryType(UniformCategory.CategoryType.REENACTMENT).build(); em.persist(period);
        uniform = Uniform.builder().name("Mũ cối").country(country).category(force).periodCategory(period)
                .images(new HashSet<>()).build(); em.persist(uniform);
        for (boolean sensitive : List.of(false, true)) {
            Image image = Image.builder().uniform(uniform).sensitive(sensitive)
                    .imageUrl(sensitive ? "https://sensitive.test/private-uniform.jpg" : "https://public.test/visible.jpg").build();
            em.persist(image); uniform.getImages().add(image);
        }
        em.persist(Comment.builder().user(admin).uniform(uniform).content("Public comment").build());
        Category category = Category.builder().name("Reference").slug("reference-test").build(); em.persist(category);
        article = Article.builder().title("Published").slug("published").status(ArticleStatus.PUBLISHED).author(admin)
                .thumbnailSensitive(true).thumbnailUrl("https://sensitive.test/private-thumbnail.jpg")
                .featured(true).publishedAt(LocalDateTime.now()).categories(new HashSet<>(Set.of(category))).build(); em.persist(article);
        for (boolean sensitive : List.of(false, true)) {
            ArticleImage image = ArticleImage.builder().article(article).referenceUniform(uniform).sensitive(sensitive)
                    .imageUrl(sensitive ? "https://sensitive.test/private-article.jpg" : "https://public.test/visible.jpg").build();
            em.persist(image); if (sensitive) hidden = image;
        }
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of(
                "secure_url", "https://sensitive.test/upload.jpg", "url", "https://public.test/upload.jpg"));
        em.flush();
    }

    @ParameterizedTest
    @CsvSource({"list,guest","list,user","list,admin","latest,guest","latest,user","latest,admin",
            "featured,guest","featured,user","featured,admin","category,guest","category,user","category,admin",
            "slug,guest","slug,user","slug,admin","images,guest","images,user","images,admin",
            "reference,guest","reference,user","reference,admin","uniform,guest","uniform,user","uniform,admin",
            "uniformImages,guest","uniformImages,user","uniformImages,admin","showcase,guest","showcase,user","showcase,admin",
            "uniformList,guest","uniformList,user","uniformList,admin","uniformCategory,guest","uniformCategory,user","uniformCategory,admin"})
    void sensitiveUrlsAreRedactedAcrossPublicResponses(String endpoint, String identity) throws Exception {
        String url = switch (endpoint) {
            case "list" -> "/api/articles";
            case "latest", "featured" -> "/api/articles/" + endpoint;
            case "category" -> "/api/articles/category/reference-test";
            case "slug" -> "/api/articles/published";
            case "images" -> "/api/articles/" + article.getId() + "/images";
            case "reference" -> "/api/articles/images/references?uniformId=" + uniform.getId();
            case "uniform" -> "/api/uniforms/" + uniform.getId();
            case "uniformImages" -> "/api/images/uniform/" + uniform.getId();
            case "uniformList" -> "/api/uniforms";
            case "uniformCategory" -> "/api/uniforms/category/" + force.getId();
            default -> "/api/uniforms/reenactment?countryId=" + country.getId() + "&categoryId=" + force.getId() + "&periodCategoryId=" + period.getId();
        };
        String body = mvc.perform(auth(get(url), identity)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andReturn().getResponse().getContentAsString();
        assertEquals(!identity.equals("guest"), body.contains("https://sensitive.test/"));
        assertFalse(body.contains("\"password\""));
        assertFalse(body.contains("\"resetToken\""));
        assertTrue(body.contains(endpoint.equals("list") || endpoint.equals("latest") || endpoint.equals("featured")
                || endpoint.equals("category") || endpoint.equals("slug") ? "thumbnailSensitive\":true" : "sensitive\":true"));
    }

    @Test void invalidTokenCannotRevealMediaAndDraftImagesAreNotPublic() throws Exception {
        mvc.perform(get("/api/articles/published").header("Authorization", "Bearer bad"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.thumbnailUrl").isEmpty());
        article.setStatus(ArticleStatus.DRAFT); em.flush();
        mvc.perform(get("/api/articles/" + article.getId() + "/images")).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/articles/" + article.getId() + "/images"), "user")).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/articles/" + article.getId() + "/images"), "admin")).andExpect(status().isOk());
        mvc.perform(get("/api/articles/images/references?uniformId=" + uniform.getId())).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void captionAutoReferenceWorksOnUploadAndPatchAndReportsUnmatchedNames() throws Exception {
        mvc.perform(auth(multipart("/api/articles/" + article.getId() + "/images")
                        .file(new MockMultipartFile("file", "test.jpg", "image/jpeg", java.util.Base64.getDecoder().decode("/9j/4AAQSkZJRgABAQAAAQABAAD/2Q==")))
                        .param("sensitive", "true").param("description", "Ảnh tư liệu\nDO VAT: mu coi"), "admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sensitive").value(true))
                .andExpect(jsonPath("$.referenceUniformId").value(uniform.getId()));
        mvc.perform(auth(patch("/api/articles/images/" + hidden.getId()), "admin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Đồ vật: MŨ CỐI\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.referenceUniformId").value(uniform.getId()));
        mvc.perform(auth(patch("/api/articles/images/" + hidden.getId()), "admin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitive\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.referenceUniformId").value(uniform.getId()))
                .andExpect(jsonPath("$.description").value("Đồ vật: MŨ CỐI"));
        mvc.perform(auth(patch("/api/articles/images/" + hidden.getId()), "admin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Đồ vật: không tồn tại\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.referenceUniformId").isEmpty())
                .andExpect(jsonPath("$.referenceWarning").isNotEmpty());
    }

    @Test void layoutAndPeriodSelectionsArePersistedAndInvalidScopesRejected() throws Exception {
        var result = mvc.perform(auth(multipart("/api/uniforms").param("name", "New showcase")
                        .param("countryId", country.getId().toString()).param("categoryId", force.getId().toString())
                        .param("periodCategoryId", period.getId().toString()), "admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.periodCategory.id").value(period.getId()));
        Image image = uniform.getImages().iterator().next();
        mvc.perform(auth(patch("/api/images/" + image.getId()), "admin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayMode\":\"FULL\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayMode").value("FULL"));
        em.flush(); assertEquals(Image.DisplayMode.FULL, em.find(Image.class, image.getId()).getDisplayMode());
        mvc.perform(get("/api/uniforms/reenactment?countryId=" + (country.getId()+1)
                        + "&categoryId=" + force.getId() + "&periodCategoryId=" + period.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test void managementApisKeepUserDataPrivateAndRequireAdmin() throws Exception {
        mvc.perform(get("/api/users/admin")).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/users/admin"), "user")).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/users/admin"), "admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].resetToken").doesNotExist());
    }

    @Test void adminCannotLockTheirOwnAccountAndLockingAnotherUserRevokesTheirToken() throws Exception {
        mvc.perform(auth(patch("/api/users/admin/" + admin.getId() + "/status?enabled=false"), "admin"))
                .andExpect(status().isBadRequest());
        assertTrue(admin.isStatus());
        User user = em.createQuery("select u from User u where u.username='user'", User.class).getSingleResult();
        String token = jwt.generateToken("user");
        mvc.perform(auth(patch("/api/users/admin/" + user.getId() + "/status?enabled=false"), "admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(false));
        mvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    <T extends AbstractMockHttpServletRequestBuilder<T>> T auth(T request, String name) {
        return name.equals("guest") ? request : request.header("Authorization", "Bearer " + jwt.generateToken(name));
    }
}
