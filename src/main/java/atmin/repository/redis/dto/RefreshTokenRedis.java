package atmin.repository.redis.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRedis {
    private String token;
    private String username;
    private long expiredAt;
    private boolean revoked;
}
