package com.caicai.auth;

import com.caicai.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoCleanupScheduler {

    private final UserRepository userRepository;

    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional
    public void cleanupDemoUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        int deleted = userRepository.deleteByIsDemoTrueAndCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired demo users", deleted);
        }
    }
}