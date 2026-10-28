package com.A509.Security;

import com.A509.Config.CorsConfig;
import com.A509.Controller.CommentController;
import com.A509.Entity.Comment;
import com.A509.Entity.Role;
import com.A509.Entity.User;
import com.A509.Repository.*;
import com.A509.Service.CommentService;
import com.A509.Untils.JwtUntilHelper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityFoundationTest.Config.class)
@TestPropertySource(properties = "app.cors.allowed-origins= http://localhost:5173, https://phamgiakhoi04.github.io, ")
@WebAppConfiguration
class SecurityFoundationTest {
    static final byte[] KEY = new byte[32];
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired CommentRepository comments;
    @Autowired UniformRepository uniforms;
    @Autowired ArticleRepository articles;
    @Autowired JwtUntilHelper jwt;
    MockMvc mvc;
    User owner;

    @BeforeEach
    void setUp() {
        reset(users, comments, uniforms, articles);
        SecurityContextHolder.clearContext();
        owner = account(1L, "owner", "USER", true);
        when(users.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(users.findByUsername("user")).thenReturn(Optional.of(account(2L, "user", "USER", true)));
        when(users.findByUsername("admin")).thenReturn(Optional.of(account(3L, "admin", "ADMIN", true)));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    static User account(long id, String name, String role, boolean enabled) {
        return User.builder().id(id).username(name).password("unused")
                .status(enabled).role(Role.builder().name(role).build()).build();
    }

    static Stream<Arguments> mutations() {
        return Stream.of("/api/articles", "/api/articles/1", "/api/articles/1/images",
                        "/api/articles/images/1", "/api/uniform-categories/1", "/api/categories/1",
                        "/api/uniforms/1", "/api/images/1", "/api/countries/1", "/api/users/admin/1/status")
                .flatMap(path -> Stream.of("POST", "PUT", "PATCH", "DELETE")
                        .flatMap(method -> Stream.of("guest", "user", "admin")
                                .map(identity -> Arguments.of(path, method, identity))));
    }

    @ParameterizedTest
    @MethodSource("mutations")
    void onlyAdminMayMutateContent(String path, String method, String identity) throws Exception {
        var request = request(HttpMethod.valueOf(method), path);
        authorize(request, identity);
        mvc.perform(request).andExpect(status().is(identity.equals("guest") ? 401 : identity.equals("user") ? 403 : 200));
    }

    static Stream<Arguments> reads() {
        return Stream.of("/api/articles", "/api/articles/latest", "/api/articles/images/references",
                        "/api/articles/1/images", "/api/uniforms", "/api/images/uniform/1", "/api/countries",
                        "/api/uniform-categories", "/api/categories", "/api/activity")
                .flatMap(path -> Stream.of("guest", "user", "admin").map(identity -> Arguments.of(path, identity)));
    }

    @ParameterizedTest
    @MethodSource("reads")
    void publicReadsRemainAccessible(String path, String identity) throws Exception {
        mvc.perform(authorize(get(path), identity)).andExpect(status().isOk());
    }

    static Stream<Arguments> privateReads() {
        return Stream.of("/api/articles/admin", "/api/users/admin", "/api/users/1", "/api/users/profile")
                .flatMap(path -> Stream.of("guest", "user", "admin").map(identity -> Arguments.of(path, identity)));
    }

    @ParameterizedTest
    @MethodSource("privateReads")
    void profileIsPersonalAndManagementIsAdminOnly(String path, String identity) throws Exception {
        int expected = identity.equals("guest") ? 401 : path.equals("/api/users/profile") || identity.equals("admin") ? 200 : 403;
        mvc.perform(authorize(get(path), identity)).andExpect(status().is(expected));
    }

    static Stream<String> invalidTokens() {
        return Stream.of("garbage", "", signed("user", -60_000, KEY),
                signed("user", 60_000, new byte[64]), signed("missing", 60_000, KEY));
    }

    @ParameterizedTest
    @MethodSource("invalidTokens")
    void invalidTokenIsAnonymousOnBothPublicAndPrivateRoutes(String token) throws Exception {
        mvc.perform(get("/api/articles").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void disablingAccountRevokesAnAlreadyIssuedTokenAndLoginDetails() throws Exception {
        String token = jwt.generateToken("user");
        mvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        when(users.findByUsername("user")).thenReturn(Optional.of(account(2L, "user", "USER", false)));
        assertFalse(context.getBean(CustomUserDetailService.class).loadUserByUsername("user").isEnabled());
        mvc.perform(get("/api/articles").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void unexpectedCredentialLookupFailureDoesNotBreakPublicRoutes() throws Exception {
        when(users.findByUsername("user")).thenThrow(new IllegalStateException("test lookup unavailable"));
        mvc.perform(authorize(get("/api/articles"), "user")).andExpect(status().isOk());
        mvc.perform(authorize(get("/api/users/profile"), "user")).andExpect(status().isUnauthorized());
    }

    @Test
    void corsUsesTrimmedAllowlistAndRejectsUnknownOrigins() throws Exception {
        mvc.perform(options("/api/articles/1").header("Origin", "https://phamgiakhoi04.github.io")
                        .header("Access-Control-Request-Method", "PATCH").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://phamgiakhoi04.github.io"));
        mvc.perform(options("/api/articles/1").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void anonymousCommentWritesAreRejectedBeforeReachingService() throws Exception {
        for (String method : List.of("POST", "PUT", "DELETE")) {
            mvc.perform(request(HttpMethod.valueOf(method), method.equals("POST") ? "/api/comments" : "/api/comments/10")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"test\",\"articleId\":1}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(comments);
    }

    @Test
    void authenticatedUsersCanCreateCommentsButCannotImpersonateTheirOwner() throws Exception {
        when(articles.findById(1L)).thenReturn(Optional.of(com.A509.Entity.Article.builder().id(1L).build()));
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(authorize(post("/api/comments"), "user").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"test\",\"articleId\":1,\"userId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(2));
    }

    @Test
    void commentOwnershipIsEnforcedAndAdminCanModerateDeletes() throws Exception {
        Comment comment = Comment.builder().id(10L).content("original").user(owner).build();
        when(comments.findById(10L)).thenReturn(Optional.of(comment));
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        for (String identity : List.of("user", "admin")) {
            mvc.perform(authorize(put("/api/comments/10"), identity)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"edited\"}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(authorize(delete("/api/comments/10"), "user")).andExpect(status().isForbidden());
        verify(comments, never()).delete(any(Comment.class));
        mvc.perform(authorize(put("/api/comments/10"), "owner")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"edited\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("edited"));
        mvc.perform(authorize(delete("/api/comments/10"), "owner")).andExpect(status().isOk());
        mvc.perform(authorize(delete("/api/comments/10"), "admin")).andExpect(status().isOk());
        verify(comments, times(2)).delete(comment);
    }

    MockHttpServletRequestBuilder authorize(MockHttpServletRequestBuilder request, String identity) {
        return identity.equals("guest") ? request : request.header("Authorization", "Bearer " + jwt.generateToken(identity));
    }

    static String signed(String name, long ttl, byte[] key) {
        return Jwts.builder().setSubject(name).setExpiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }

    @TestConfiguration
    @EnableWebMvc
    @Import({CustomFilterSecurity.class, CustomJwtFilter.class, CustomUserDetailService.class, CorsConfig.class,
            CommentController.class, CommentService.class, ProbeController.class})
    static class Config {
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
        @Bean CommentRepository commentRepository() { return mock(CommentRepository.class); }
        @Bean UniformRepository uniformRepository() { return mock(UniformRepository.class); }
        @Bean ArticleRepository articleRepository() { return mock(ArticleRepository.class); }
        @Bean JwtUntilHelper jwtUntilHelper() {
            var helper = new JwtUntilHelper();
            ReflectionTestUtils.setField(helper, "secret", java.util.Base64.getEncoder().encodeToString(KEY));
            return helper;
        }
    }

    // A probe intentionally accepts every method: the real security chain must block unauthorized calls.
    // Comment requests use the real controller and service above.
    @RestController
    @TestComponent
    static class ProbeController {
        @RequestMapping("/api/**") String ok() { return "ok"; }
    }
}
