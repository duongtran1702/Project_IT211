package atmin.infrastructure.scheduler;

import atmin.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Dọn dẹp định kỳ các Refresh Token đã hết hạn sử dụng.
     * Chạy tự động vào lúc 12h đêm (00:00) mỗi ngày.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanExpiredTokens() {
        log.info("Scheduler: Running cleanup for expired refresh tokens...");
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
