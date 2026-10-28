package com.A509.Service;
import com.A509.Entity.*;
import com.A509.Security.MediaCacheFilter;
import com.A509.Untils.JwtUntilHelper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.MediaType;
import java.util.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest @ActiveProfiles("test")
@TestPropertySource(locations="classpath:application-test.properties")
@Transactional
class PlatformFeaturesTest {
 @Autowired EntityManager em;@Autowired WebApplicationContext context;@Autowired JwtUntilHelper jwt;@Autowired TranslationService translations;@Autowired AnalyticsService analytics;@Autowired MediaCacheFilter cache;
 MockMvc mvc;Article article;User user;Country country;Uniform uniform;ArticleImage image;String adminToken;
 @BeforeEach void setup(){
  mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).addFilters(cache).build();
  Role admin=Role.builder().name("ADMIN").build();em.persist(admin);user=User.builder().username("platform-admin").email("platform@example.test").password("unused").role(admin).build();em.persist(user);adminToken=jwt.generateToken(user.getUsername());
  article=Article.builder().title("Nghiên cứu mũ cối").slug("platform-article").content("Toàn bộ nội dung tiếng Việt").status(ArticleStatus.PUBLISHED).author(user).build();em.persist(article);
  Article draft=Article.builder().title("Không công khai mật").slug("secret-draft").content("Private draft text").author(user).build();em.persist(draft);
  country=Country.builder().countryName("Việt Nam").continent("Châu Á").build();em.persist(country);
  UniformCategory root=UniformCategory.builder().categoryName("Phục dựng trang phục").categoryType(UniformCategory.CategoryType.REENACTMENT).build();em.persist(root);
  UniformCategory force=UniformCategory.builder().categoryName("Quân đội nhân dân Việt Nam").parent(root).countryId(country.getId()).categoryType(UniformCategory.CategoryType.REENACTMENT).build();em.persist(force);
  UniformCategory period=UniformCategory.builder().categoryName("Thời kỳ tiền khởi nghĩa").slug("thoi-ky-tien-khoi-nghia").parent(force).countryId(country.getId()).categoryType(UniformCategory.CategoryType.REENACTMENT).build();em.persist(period);
  uniform=Uniform.builder().name("Áo phục dựng").country(country).category(force).periodCategory(period).build();em.persist(uniform);
  image=ArticleImage.builder().article(article).imageUrl("https://sensitive.test/secret.jpg").sensitive(true).description("Đồ vật trong ảnh").sortOrder(0).build();em.persist(image);
  em.flush();
 }
 @Test void adminApisRejectGuestsAndNormalUsers()throws Exception{
  mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/translations/ARTICLE")).andExpect(status().isUnauthorized());
  Role role=Role.builder().name("USER").build();em.persist(role);User reader=User.builder().username("platform-reader").email("reader@example.test").password("unused").role(role).build();em.persist(reader);em.flush();
  mvc.perform(get("/api/admin/overview").header("Authorization","Bearer "+jwt.generateToken(reader.getUsername()))).andExpect(status().isForbidden());
  mvc.perform(put("/api/admin/translations/ARTICLE/"+article.getId()).header("Authorization","Bearer "+jwt.generateToken(reader.getUsername())).contentType(MediaType.APPLICATION_JSON).content("{\"fields\":{},\"source\":{}}")).andExpect(status().isForbidden());
 }
 @Test void manualTranslationsApplyToFullArticleAndStaySeparateFromSource()throws Exception{
  translations.save("ARTICLE",article.getId(),new TranslationService.Update(Map.of("title","Research on helmets","content","The complete English article"),Map.of("title",article.getTitle(),"content",article.getContent())));
  mvc.perform(get("/api/articles/"+article.getSlug()).header("X-A509-Language","en")).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Research on helmets")).andExpect(jsonPath("$.content").value("The complete English article")).andExpect(jsonPath("$.translationStatus").value("complete"));
  mvc.perform(get("/api/articles/"+article.getSlug())).andExpect(jsonPath("$.title").value(article.getTitle()));
 }
 @Test void translationCannotSetImageUrlAndCannotOverwriteChangedSource(){
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->translations.save("ARTICLE_IMAGE",image.getId(),new TranslationService.Update(Map.of("imageUrl","https://attack.test"),Map.of())));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->translations.save("ARTICLE",article.getId(),new TranslationService.Update(Map.of("title","Changed"),Map.of("title","outdated source"))));
 }
 @Test void localizedCaptionsDoNotRevealSensitiveUrls()throws Exception{
  translations.save("ARTICLE_IMAGE",image.getId(),new TranslationService.Update(Map.of("description","Object in the photo"),Map.of("description",image.getDescription())));
  mvc.perform(get("/api/articles/"+article.getId()+"/images").header("X-A509-Language","en")).andExpect(status().isOk()).andExpect(content().string(not(containsString("secret.jpg")))).andExpect(content().string(containsString("Object in the photo")));
 }
 @Test void countryTranslationPreservesRoutingName()throws Exception{
  translations.save("COUNTRY",country.getId(),new TranslationService.Update(Map.of("countryName","Vietnam"),Map.of("countryName","Việt Nam")));
  mvc.perform(get("/api/countries/"+country.getId()).header("X-A509-Language","en")).andExpect(jsonPath("$.countryName").value("Vietnam")).andExpect(jsonPath("$.sourceName").value("Việt Nam"));
 }
 @Test void staleTranslationFallsBackAndIsFlagged()throws Exception{
  translations.save("ARTICLE",article.getId(),new TranslationService.Update(Map.of("title","Old title"),Map.of("title",article.getTitle())));
  article.setTitle("Bản gốc đã sửa");em.flush();
  mvc.perform(get("/api/articles/"+article.getSlug()).header("X-A509-Language","en")).andExpect(jsonPath("$.title").value(article.getTitle())).andExpect(jsonPath("$.translationStatus").value("missing"));
  assertTrue(((List<?>)translations.read("ARTICLE",article.getId()).get("stale")).contains("title"));
 }
 @Test void searchNormalizesAccentsAndExcludesDrafts()throws Exception{
  mvc.perform(get("/api/search").param("q","nghien cuu mu coi")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].title").value(article.getTitle()));
  mvc.perform(get("/api/search").param("q","Private draft text")).andExpect(jsonPath("$.total").value(0));
  mvc.perform(get("/api/search").param("q","' OR 1=1 --")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
 }
 @Test void englishSearchFindsTranslatedContentAndReenactmentUsesCanonicalRoute()throws Exception{
  translations.save("ARTICLE",article.getId(),new TranslationService.Update(Map.of("title","Research on helmets","content","English unique content"),Map.of("title",article.getTitle(),"content",article.getContent())));
  mvc.perform(get("/api/search").param("q","English unique").header("X-A509-Language","en")).andExpect(jsonPath("$.items[0].title").value("Research on helmets"));
  mvc.perform(get("/api/search").param("q","ao phuc dung").param("type","reenactment")).andExpect(jsonPath("$.items[0].href").value("/hoat-dong/phuc-dung/viet-nam/quan-doi-nhan-dan-viet-nam/thoi-ky-tien-khoi-nghia#content-"+uniform.getId()));
 }
 @Test void translatedPublicActivityUsesPublishedTitle()throws Exception{
  ActivityLog log=new ActivityLog();log.setArticle(article);log.setUser(user);log.setAction("UPDATE");log.setDescription("Internal detail must never appear");em.persist(log);em.flush();
  translations.save("ARTICLE",article.getId(),new TranslationService.Update(Map.of("title","Research on helmets"),Map.of("title",article.getTitle())));
  mvc.perform(get("/api/activity").header("X-A509-Language","en")).andExpect(status().isOk()).andExpect(jsonPath("$[0].description").value("Updated: Research on helmets")).andExpect(content().string(not(containsString("Internal detail"))));
 }
 @Test void analyticsCountsRealEventsDeduplicatesAndIgnoresSpoofedCountry()throws Exception{
  String visitor=UUID.randomUUID().toString(),session=UUID.randomUUID().toString(),event=UUID.randomUUID().toString();
  MockHttpServletRequest request=new MockHttpServletRequest();request.addHeader("User-Agent","Mozilla iPhone Mobile");request.addHeader("X-A509-Country","US");
  var visit=new AnalyticsService.Visit(event,visitor,session,"/tin-tuc/test?email=private#token","https://example.test/path?secret=hidden");analytics.record(visit,request);analytics.record(visit,request);
  mvc.perform(get("/api/admin/overview").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.pageviews").value(1)).andExpect(jsonPath("$.visitors").value(1)).andExpect(jsonPath("$.device[0].label").value("phone")).andExpect(jsonPath("$.country[0].label").value("ZZ")).andExpect(jsonPath("$.referrer[0].label").value("example.test")).andExpect(content().string(not(containsString("private")))).andExpect(content().string(not(containsString("hidden")))).andExpect(jsonPath("$.daily.length()").value(30));
 }
 @Test void analyticsRespectsDntAndExcludesPrivatePages(){
  MockHttpServletRequest request=new MockHttpServletRequest();request.addHeader("DNT","1");analytics.record(visit("/"),request);
  request.removeHeader("DNT");analytics.record(visit("/admin/dashboard"),request);analytics.record(visit("/reset-password?token=private"),request);
  assertEquals(0L,analytics.overview(7).get("pageviews"));
 }
 AnalyticsService.Visit visit(String path){return new AnalyticsService.Visit(UUID.randomUUID().toString(),UUID.randomUUID().toString(),UUID.randomUUID().toString(),path,"");}
}
