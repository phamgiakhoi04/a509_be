package com.A509.Service;

import com.A509.Entity.User;
import com.A509.Repository.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResetMailConfigurationTest {
    @ParameterizedTest
    @ValueSource(strings = {"https://phamgiakhoi04.github.io/A509", "https://phamgiakhoi04.github.io/A509/"})
    void bothResetSendersPreserveFrontendBasePathAndUseConfiguredMailbox(String baseUrl) {
        JavaMailSender mail = mock(JavaMailSender.class);
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail("reader@example.test")).thenReturn(Optional.of(User.builder().build()));
        var active = new ForgotPasswordService();
        ReflectionTestUtils.setField(active, "userRepository", users);
        ReflectionTestUtils.setField(active, "mailSender", mail);
        ReflectionTestUtils.setField(active, "mailUsername", "sender@example.test");
        ReflectionTestUtils.setField(active, "frontendUrl", baseUrl);
        active.forgotPassword("reader@example.test");

        var legacy = new ResetPasswordService();
        ReflectionTestUtils.setField(legacy, "mailSender", mail);
        ReflectionTestUtils.setField(legacy, "mailUsername", "sender@example.test");
        ReflectionTestUtils.setField(legacy, "frontendUrl", baseUrl);
        legacy.sendResetPasswordEmail("reader@example.test", "test-token");

        var messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(2)).send(messages.capture());
        for (var message : messages.getAllValues()) {
            assertEquals("sender@example.test", message.getFrom());
            assertTrue(message.getText().contains("https://phamgiakhoi04.github.io/A509/reset-password?token="));
            assertFalse(message.getText().contains("localhost"));
        }
    }
}
