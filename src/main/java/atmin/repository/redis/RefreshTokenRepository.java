package atmin.repository.redis;

import atmin.repository.redis.dto.RefreshTokenRedis;
import atmin.entity.User;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenRepository {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOKEN_PREFIX = "refresh_token:";
    private static final String USER_SET_PREFIX = "user_refresh_tokens:";

    /**
     * Lưu thông tin Refresh Token vào Redis.
     * 1. Lưu metadata dạng JSON tại key 'refresh_token:<token>' với thời gian sống TTL.
     * 2. Nếu token còn hoạt động (chưa bị thu hồi), thêm token đó vào Set đại diện của người dùng
     * tại key 'user_refresh_tokens:<username>' để theo dõi và thu hồi hàng loạt khi cần.
     * 3. Nếu token đã hết hạn hoặc bị thu hồi, tự động xóa nó ra khỏi Redis và Set của người dùng.
     */
    public void save(RefreshTokenRedis tokenRedis) {
        try {
            String tokenKey = TOKEN_PREFIX + tokenRedis.getToken();
            String json = objectMapper.writeValueAsString(tokenRedis);

            // Tính TTL bằng hiệu số giữa thời điểm hết hạn và thời điểm hiện tại (mili-giây)
            long ttlMs = tokenRedis.getExpiredAt() - System.currentTimeMillis();
            if (ttlMs > 0) {
                // Lưu chuỗi JSON của Token vào Redis kèm TTL
                redisTemplate.opsForValue().set(tokenKey, json, ttlMs, TimeUnit.MILLISECONDS);

                String userSetKey = USER_SET_PREFIX + tokenRedis.getUsername();
                if (!tokenRedis.isRevoked()) {
                    // Thêm token vào Set hoạt động của user và gia hạn TTL của Set
                    redisTemplate.opsForSet().add(userSetKey, tokenRedis.getToken());
                    redisTemplate.expire(userSetKey, ttlMs, TimeUnit.MILLISECONDS);
                } else {
                    // Nếu token bị thu hồi (revoked = true), loại bỏ khỏi Set hoạt động
                    redisTemplate.opsForSet().remove(userSetKey, tokenRedis.getToken());
                }
            } else {
                // Nếu token đã hết hạn, tiến hành xóa sạch key và phần tử trong Set
                redisTemplate.delete(tokenKey);
                redisTemplate.opsForSet().remove(USER_SET_PREFIX + tokenRedis.getUsername(), tokenRedis.getToken());
            }
        } catch (Exception e) {
            log.error("Failed to serialize RefreshTokenRedis", e);
        }
    }

    /**
     * Tìm kiếm thông tin Refresh Token bằng chuỗi token.
     * Đọc JSON từ Redis và chuyển đổi ngược lại thành đối tượng RefreshTokenRedis.
     */
    public Optional<RefreshTokenRedis> findByToken(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        String json = redisTemplate.opsForValue().get(tokenKey);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, RefreshTokenRedis.class));
        } catch (Exception e) {
            log.error("Failed to deserialize RefreshTokenRedis", e);
            return Optional.empty();
        }
    }

    /**
     * Tìm tất cả các Refresh Token đang hoạt động (chưa bị thu hồi & chưa hết hạn) của User.
     */
    public List<RefreshTokenRedis> findAllActiveByUser(User user) {
        return findAllActiveByUsername(user.getUsername());
    }

    /**
     * Tìm tất cả các Refresh Token đang hoạt động của User dựa trên Username.
     * Đồng thời tự dọn dẹp (Self-Cleansing) các token rác trong Set đã bị Redis xóa do hết TTL
     * hoặc các token đã hết hạn/bị thu hồi nhưng chưa gỡ khỏi Set.
     */
    public List<RefreshTokenRedis> findAllActiveByUsername(String username) {
        String userSetKey = USER_SET_PREFIX + username;
        Set<String> tokens = redisTemplate.opsForSet().members(userSetKey);
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<RefreshTokenRedis> activeTokens = new ArrayList<>();
        for (String token : tokens) {
            Optional<RefreshTokenRedis> tokenOpt = findByToken(token);
            if (tokenOpt.isPresent()) {
                RefreshTokenRedis t = tokenOpt.get();
                if (t.getExpiredAt() > System.currentTimeMillis()) {
                    activeTokens.add(t);
                } else {
                    // Token đã hết hạn -> gỡ khỏi Set hoạt động
                    redisTemplate.opsForSet().remove(userSetKey, token);
                }
            } else {
                // Token đã bị Redis tự động xóa do hết hạn TTL -> gỡ phần tử khỏi Set hoạt động
                redisTemplate.opsForSet().remove(userSetKey, token);
            }
        }
        return activeTokens;
    }

    /**
     * Lưu danh sách các Refresh Token xuống Redis (tiện ích hỗ trợ thu hồi hàng loạt).
     */
    public void saveAll(List<RefreshTokenRedis> tokens) {
        for (RefreshTokenRedis token : tokens) {
            save(token);
        }
    }

}
