package atmin.controller.auth.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
}