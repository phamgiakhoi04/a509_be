package com.A509.Security;
import com.A509.Entity.*;
import com.A509.Repository.UserRepository;
import com.A509.Service.ForgotPasswordService;
import com.A509.Untils.JwtUntilHelper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.mockito.ArgumentCaptor;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HardeningTest {
 @Test void resetStoresOnlyHashExpiresAndRevokesOldJwt(){
  UserRepository repo=mock(UserRepository.class);JavaMailSender mail=mock(JavaMailSender.class);
  User user=User.builder().username("reader").email("reader@example.test").password("old").role(Role.builder().name("USER").build()).build();when(repo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
  ForgotPasswordService reset=new ForgotPasswordService();ReflectionTestUtils.setField(reset,"userRepository",repo);ReflectionTestUtils.setField(reset,"mailSender",mail);ReflectionTestUtils.setField(reset,"passwordEncoder",new BCryptPasswordEncoder());ReflectionTestUtils.setField(reset,"mailUsername","sender@example.test");ReflectionTestUtils.setField(reset,"frontendUrl","https://example.test/");
  JwtUntilHelper jwt=new JwtUntilHelper();ReflectionTestUtils.setField(jwt,"secret","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");String token=jwt.generateToken("reader");
  reset.forgotPassword(user.getEmail());ArgumentCaptor<SimpleMailMessage> message=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(mail).send(message.capture());
  String raw=message.getValue().getText().split("token=")[1].split("\\s")[0];assertNotEquals(raw,user.getResetToken());assertEquals(64,user.getResetToken().length());assertTrue(user.getResetTokenExpiry().isAfter(LocalDateTime.now().plusMinutes(14)));
  when(repo.findByResetToken(user.getResetToken())).thenReturn(Optional.of(user));reset.resetPassword(raw,"NewPassword!23");
  assertEquals(1,user.getTokenVersion());assertNull(user.getResetToken());assertNull(user.getResetTokenExpiry());assertTrue(new BCryptPasswordEncoder().matches("NewPassword!23",user.getPassword()));assertFalse(jwt.verifyToken(token,new VersionedUserDetails(user)));
  assertTrue(jwt.verifyToken(jwt.generateToken("reader",1),new VersionedUserDetails(user)));
 }
 @Test void unknownEmailHasSameResponseAndNoMail(){
  UserRepository repo=mock(UserRepository.class);JavaMailSender mail=mock(JavaMailSender.class);when(repo.findByEmail(anyString())).thenReturn(Optional.empty());ForgotPasswordService reset=new ForgotPasswordService();ReflectionTestUtils.setField(reset,"userRepository",repo);ReflectionTestUtils.setField(reset,"mailSender",mail);
  assertEquals("Nếu email hợp lệ, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.",reset.forgotPassword("unknown@example.test"));verifyNoInteractions(mail);
 }
 @Test void passwordPolicyBoundsBcryptBytes(){assertThrows(IllegalArgumentException.class,()->PasswordPolicy.validate("short"));assertThrows(IllegalArgumentException.class,()->PasswordPolicy.validate("đ".repeat(37)));assertDoesNotThrow(()->PasswordPolicy.validate("Password!1"));}
 @Test void uploadRejectsSpoofedImageSvgAndOversize()throws Exception{
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->UploadPolicy.validate(new MockMultipartFile("file","fake.jpg","image/jpeg","<script>alert(1)</script>".getBytes())));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->UploadPolicy.validate(new MockMultipartFile("file","vector.svg","image/svg+xml","<svg></svg>".getBytes())));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->UploadPolicy.validate(new MockMultipartFile("file","huge.png","image/png",new byte[5*1024*1024+1])));
  assertDoesNotThrow(()->UploadPolicy.validate(new MockMultipartFile("file","real.png","image/png",Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jZQAAAABJRU5ErkJggg=="))));
 }
 @Test void limiterBlocksRepeatedLoginButAllowsAnotherIpAndPreflight()throws Exception{
  RequestRateLimitFilter filter=new RequestRateLimitFilter();
  for(int i=0;i<11;i++){MockHttpServletRequest request=new MockHttpServletRequest("POST","/api/auth/login");request.setRemoteAddr("198.51.100.1");MockHttpServletResponse response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());assertEquals(i<10?200:429,response.getStatus());if(i==10)assertNotNull(response.getHeader("Retry-After"));}
  MockHttpServletRequest request=new MockHttpServletRequest("POST","/api/auth/login");request.setRemoteAddr("198.51.100.2");MockHttpServletResponse response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());assertEquals(200,response.getStatus());
  request=new MockHttpServletRequest("OPTIONS","/api/auth/login");request.setRemoteAddr("198.51.100.1");response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());assertEquals(200,response.getStatus());
 }
 @Test void temporaryAccountDatabaseFailureReturns503InsteadOfExpiringSession()throws Exception{
  CustomUserDetailService details=mock(CustomUserDetailService.class);when(details.loadUserByUsername("reader")).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("temporary outage"));
  JwtUntilHelper jwt=new JwtUntilHelper();ReflectionTestUtils.setField(jwt,"secret","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");CustomJwtFilter filter=new CustomJwtFilter();ReflectionTestUtils.setField(filter,"userDetailsService",details);ReflectionTestUtils.setField(filter,"jwtUntilHelper",jwt);
  MockHttpServletRequest request=new MockHttpServletRequest("GET","/api/users/profile");request.addHeader("Authorization","Bearer "+jwt.generateToken("reader"));MockHttpServletResponse response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());assertEquals(503,response.getStatus());assertFalse(response.getContentAsString().contains("temporary outage"));
 }
}
