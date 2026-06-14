package atmin.repository.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class TokenBlacklistRepository {
    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    /**
     * Đưa Access Token vào danh sách đen (Blacklist) trong Redis khi người dùng đăng xuất.
     * Sử dụng thời hạn sống còn lại của Token (expiryDurationMs) làm TTL để Redis tự động xóa khi hết hạn.
     */
    public void blacklistToken(String token, long expiryDurationMs) {
        if (expiryDurationMs > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    "blacklisted",
                    expiryDurationMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Kiểm tra xem Access Token có nằm trong danh sách đen trên Redis hay không.
     * Được gọi ở bộ lọc JwtAuthenticationFilter để chặn các token đã đăng xuất.
     */
    public boolean existsByToken(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
