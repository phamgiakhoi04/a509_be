package com.A509.Service;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
@Service
public class PasswordResetDispatch {
    private final ForgotPasswordService service;
    private final ThreadPoolExecutor queue = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(50), new ThreadPoolExecutor.AbortPolicy());
    public PasswordResetDispatch(ForgotPasswordService service) { this.service = service; }
    public void submit(String email) {
        try { queue.execute(() -> { try { service.forgotPassword(email); } catch (RuntimeException ignored) { } }); }
        catch (RejectedExecutionException ignored) { /* Generic response; bounded mail queue. */ }
    }
    @PreDestroy public void close() { queue.shutdownNow(); }
}
