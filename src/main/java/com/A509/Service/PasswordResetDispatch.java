package com.A509.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class PasswordResetDispatch {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetDispatch.class);
    private final ForgotPasswordService service;
    private final boolean awaitDelivery;
    private final ThreadPoolExecutor queue = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50), new ThreadPoolExecutor.AbortPolicy());

    public PasswordResetDispatch(ForgotPasswordService service,
                                 @Value("${app.mail.await-delivery:false}") boolean awaitDelivery) {
        this.service = service;
        this.awaitDelivery = awaitDelivery;
    }

    public void submit(String email) {
        if (awaitDelivery) {
            deliver(email);
            return;
        }
        try {
            queue.execute(() -> deliver(email));
        } catch (RejectedExecutionException ex) {
            log.warn("Password reset mail queue unavailable");
        }
    }

    private void deliver(String email) {
        try {
            service.forgotPassword(email);
        } catch (RuntimeException ex) {
            log.warn("Password reset request could not be processed");
        }
    }

    @PreDestroy
    public void close() {
        queue.shutdownNow();
    }
}
