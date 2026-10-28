package com.A509.Service;

import com.A509.Entity.*;
import com.A509.Repository.ActivityLogRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class PublicActivityTest {
    @MockitoBean(name = "syncUniformCategoryNames") CommandLineRunner initializer;
    @Autowired EntityManager em;
    @Autowired ActivityLogService service;
    @Autowired ActivityLogRepository logs;
    @Autowired WebApplicationContext context;

    @Test
    void databaseFiltersBeforeLimitAndPublicFeedDoesNotExposeAdministrativeDetails() throws Exception {
        Role role = Role.builder().name("ADMIN").build();
        em.persist(role);
        User author = User.builder().username("private-admin").email("private@example.test")
                .password("unused").role(role).build();
        em.persist(author);
        Article published = article("visible", ArticleStatus.PUBLISHED, author);
        Article draft = article("private-draft", ArticleStatus.DRAFT, author);
        Article archived = article("private-archive", ArticleStatus.ARCHIVED, author);
        Article deleted = article("private-deleted", ArticleStatus.PUBLISHED, author);
        deleted.setDeletedAt(LocalDateTime.now());
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        // More than ten newer private events must not displace visible results.
        for (int i = 0; i < 12; i++) {
            log(published, author, "UPDATE", start.plusMinutes(i));
            log(draft, author, "UPDATE", start.plusDays(1).plusMinutes(i));
        }
        log(archived, author, "CREATE", start.plusDays(2));
        log(deleted, author, "ADD_IMAGE", start.plusDays(2));
        log(published, author, "DELETE", start.plusDays(2));
        em.flush();
        em.clear();

        var feed = service.getLatestActivities();
        assertEquals(10, feed.size());
        for (var entry : feed) {
            assertEquals("visible", entry.getArticleSlug());
            assertEquals("Cập nhật: visible", entry.getDescription());
            assertNull(entry.getUserId());
            assertNull(entry.getUsername());
            assertFalse(entry.getDescription().contains("private"));
        }
        assertTrue(feed.getFirst().getCreatedAt().isAfter(feed.getLast().getCreatedAt()));
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build()
                .perform(get("/api/activity").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].articleSlug").value("visible"))
                .andExpect(jsonPath("$[0].username").isEmpty())
                .andExpect(jsonPath("$[0].userId").isEmpty());
    }

    private Article article(String slug, ArticleStatus status, User author) {
        Article article = Article.builder().title(slug).slug(slug).status(status).author(author).build();
        em.persist(article);
        return article;
    }

    private void log(Article article, User author, String action, LocalDateTime created) {
        em.persist(ActivityLog.builder().article(article).user(author).action(action)
                .description("private administrative details / former draft title").createdAt(created).build());
    }
}
