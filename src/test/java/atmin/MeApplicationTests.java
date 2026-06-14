package atmin;

import atmin.repository.redis.RefreshTokenRepository;
import atmin.repository.redis.dto.RefreshTokenRedis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MeApplicationTests {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void testRedisSaveAndLoad() {
        String testToken = "test_token_12345";
        RefreshTokenRedis token = RefreshTokenRedis.builder()
                .token(testToken)
                .username("test_user")
                .expiredAt(System.currentTimeMillis() + 60000)
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);

        Optional<RefreshTokenRedis> loadedOpt = refreshTokenRepository.findByToken(testToken);
        assertTrue(loadedOpt.isPresent(), "Token should be found in Redis!");
        
        RefreshTokenRedis loaded = loadedOpt.get();
        assertEquals("test_user", loaded.getUsername());
        assertFalse(loaded.isRevoked());
    }

}
